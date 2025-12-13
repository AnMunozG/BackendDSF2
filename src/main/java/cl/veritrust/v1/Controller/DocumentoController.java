package cl.veritrust.v1.Controller;

import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.DTO.DocumentoDTO;
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
import jakarta.validation.Valid;
import java.net.URI;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map; // Agregado para devolver mensajes JSON en errores

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/documentos")
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
    public ResponseEntity<DocumentoDTO> uploadFile(@RequestParam("file") MultipartFile file,
                                                @RequestParam("userId") Long userId) {
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
        return ResponseEntity.ok(toDTO(guardado));
    }

    @PostMapping
    public ResponseEntity<DocumentoDTO> createDocumento(@Valid @RequestBody DocumentoDTO dto) {
        Documento d = toEntity(dto);
        Documento creado = documentoService.CrearDocumento(d);
        URI location = URI.create("/api/documentos/" + creado.getId());
        return ResponseEntity.created(location).body(toDTO(creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoDTO> updateDocumento(@PathVariable Long id, @Valid @RequestBody DocumentoDTO dto) {
        Documento detalles = toEntity(dto);
        Documento actualizado = documentoService.ActualizarDocumento(id, detalles);
        return ResponseEntity.ok(toDTO(actualizado));
    }

    // MEJORA #3: Cambiamos el retorno a ResponseEntity<?> para poder enviar errores de texto si falla la validación
    @PostMapping("/{id}/firmar")
    public ResponseEntity<?> firmarDocumento(@PathVariable Long id) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        
        // --- INICIO MEJORA #3: PREVENIR REFIRMA ---
        if (doc.isFirmado()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("mensaje", "El documento ya ha sido firmado previamente."));
        }
        // --- FIN MEJORA #3 ---

        // delegar a componente de firma (actualmente marca visual con firmante fijo)
        Documento actualizado = firmarDoc.signDocumento(doc);
        return ResponseEntity.ok(toDTO(actualizado));
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
    public ResponseEntity<List<DocumentoDTO>> listByUser(@PathVariable Long userId) {
        List<Documento> filtered = documentoService.ObtenerDocumentosPorUsuario(userId);
        List<DocumentoDTO> dtos = filtered.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocumento(@PathVariable Long id) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        
        // Borrar el archivo original (ya existía)
        fileStorageService.deleteFile(doc.getNombreAlmacenado());

        // --- INICIO MEJORA #1: EVITAR ARCHIVOS HUÉRFANOS ---
        // Verificar si existe un archivo firmado asociado y borrarlo también
        if (doc.getNombreFirmado() != null) {
            fileStorageService.deleteFile(doc.getNombreFirmado());
        }
        // --- FIN MEJORA #1 ---

        documentoService.EliminarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    private DocumentoDTO toDTO(Documento d) {
        if (d == null) return null;
        DocumentoDTO dto = new DocumentoDTO();
        dto.setId(d.getId());
        dto.setNombreOriginal(d.getNombreOriginal());
        dto.setNombreAlmacenado(d.getNombreAlmacenado());
        dto.setTipoContenido(d.getTipoContenido());
        dto.setTamano(d.getTamano());
        dto.setFechaSubida(d.getFechaSubida());
        dto.setFirmado(d.isFirmado());
        dto.setNombreFirmado(d.getNombreFirmado());
        if (d.getUsuario() != null) dto.setUsuarioId(d.getUsuario().getId());
        return dto;
    }

    private Documento toEntity(DocumentoDTO dto) {
        if (dto == null) return null;
        Documento d = new Documento();
        d.setId(dto.getId());
        d.setNombreOriginal(dto.getNombreOriginal());
        d.setNombreAlmacenado(dto.getNombreAlmacenado());
        d.setTipoContenido(dto.getTipoContenido());
        d.setTamano(dto.getTamano());
        d.setFechaSubida(dto.getFechaSubida());
        d.setFirmado(dto.isFirmado());
        d.setNombreFirmado(dto.getNombreFirmado());
        if (dto.getUsuarioId() != null) {
            Usuario u = usuarioService.ObtenerUsuarioPorId(dto.getUsuarioId());
            d.setUsuario(u);
        }
        return d;
    }

}