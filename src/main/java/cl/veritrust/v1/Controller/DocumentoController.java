package cl.veritrust.v1.Controller;

import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Service.DocumentoService;
import cl.veritrust.v1.Service.FileStorageService;
import cl.veritrust.v1.Service.UsuarioService;
import cl.veritrust.v1.Components.FirmarDoc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "http://localhost:5173") // <--- ESTO FALTA
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private FirmarDoc firmarDoc;

    @PostMapping("/upload")
    public ResponseEntity<Documento> uploadFile(@RequestParam("file") MultipartFile file,
                                                @RequestParam("userId") Long userId) {
        // validar tipo simple
        String tipoContenido = file.getContentType();
        if (tipoContenido == null ||
                !(tipoContenido.equals("application/pdf") || tipoContenido.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            return ResponseEntity.badRequest().build();
        }

        String nombreAlmacenado = fileStorageService.storeFile(file);

        Documento doc = new Documento();
        doc.setNombreOriginal(file.getOriginalFilename());
        doc.setNombreAlmacenado(nombreAlmacenado);
        doc.setTipoContenido(tipoContenido);
        doc.setTamano(file.getSize());
        doc.setFechaSubida(LocalDateTime.now());
        doc.setFirmado(false);

        try {
            Usuario usuario = usuarioService.ObtenerUsuarioPorId(userId);
            doc.setUsuario(usuario);
        } catch (Exception ex) {
            // si no existe usuario, limpiar archivo y devolver error
            fileStorageService.deleteFile(nombreAlmacenado);
            return ResponseEntity.badRequest().build();
        }

        Documento guardado = documentoService.CrearDocumento(doc);
        return ResponseEntity.ok(guardado);
    }

    @PostMapping("/{id}/firmar")
    public ResponseEntity<Documento> firmarDocumento(@PathVariable Long id,
                                                     @RequestParam("signer") String signer,
                                                     @RequestParam(value = "modo", defaultValue = "visual") String modo) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        // delegar a componente de firma (modo: visual | digital)
        Documento actualizado = firmarDoc.signDocumento(doc, signer, modo);
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        Resource resource = fileStorageService.loadFileAsResource(doc.getNombreAlmacenado());
        String archivoCodificado = URLEncoder.encode(doc.getNombreOriginal(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + archivoCodificado)
                .body(resource);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Documento>> listByUser(@PathVariable Long userId) {
        List<Documento> filtered = documentoService.ObtenerDocumentosPorUsuario(userId);
        return ResponseEntity.ok(filtered);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocumento(@PathVariable Long id) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        fileStorageService.deleteFile(doc.getNombreAlmacenado());
        documentoService.EliminarDocumento(id);
        return ResponseEntity.noContent().build();
    }
}