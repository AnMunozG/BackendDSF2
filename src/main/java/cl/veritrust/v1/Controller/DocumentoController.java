package cl.veritrust.v1.Controller;

import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.DTO.DocumentoDTO;
import cl.veritrust.v1.Service.DocumentoService;
import cl.veritrust.v1.Service.FileStorageService;
import cl.veritrust.v1.Service.UsuarioService;
import cl.veritrust.v1.Components.FirmarDoc;
import cl.veritrust.v1.Security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.net.URI;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/documento")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private FirmarDoc firmarDoc;
    @Autowired
    private SecurityUtil securityUtil;
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

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
        // Si tiene ruta de almacenamiento (documento firmado desde frontend), eliminarlo
        if (doc.getRutaAlmacenamiento() != null) {
            fileStorageService.deleteFileByRelativePath(doc.getRutaAlmacenamiento());
        }
        // --- FIN MEJORA #1 ---

        documentoService.EliminarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/documentos/firmado
     * Guarda un documento firmado desde el frontend
     */
    @PostMapping("/firmado")
    public ResponseEntity<?> guardarDocumentoFirmado(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("nombreOriginal") String nombreOriginal,
            @RequestParam("hash") String hash,
            @RequestParam("fechaFirma") String fechaFirmaStr,
            @RequestParam(value = "tipoArchivo", required = false, defaultValue = "pdf") String tipoArchivo) {
        
        String rutaAlmacenamiento = null;
        
        try {
            // Validar autenticación
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            System.out.println("Usuario autenticado: " + usuarioAutenticado.getId() + " - " + usuarioAutenticado.getRut());
            
            // Validar archivo
            if (archivo == null || archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo es requerido"));
            }
            
            // Validar tipo de archivo (solo PDF)
            String contentType = archivo.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Solo se permiten archivos PDF"));
            }
            
            // Validar tamaño máximo (10MB)
            if (archivo.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo excede el tamaño máximo de 10MB"));
            }
            
            // Validar hash
            if (hash == null || hash.trim().isEmpty() || hash.length() != 64) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El hash SHA-256 es requerido y debe tener 64 caracteres"));
            }
            
            // Validar nombre original
            if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El nombre original del archivo es requerido"));
            }
            
            // Parsear fecha de firma
            LocalDateTime fechaFirma;
            try {
                fechaFirma = LocalDateTime.parse(fechaFirmaStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de fecha inválido. Use formato ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)"));
            }
            
            // Sanitizar nombre original
            String nombreOriginalSanitizado = sanitizeFileName(nombreOriginal);
            
            // Normalizar tipoArchivo (si viene vacío o null, usar "pdf")
            if (tipoArchivo == null || tipoArchivo.trim().isEmpty()) {
                tipoArchivo = "pdf";
            }
            
            System.out.println("Guardando archivo para usuario: " + usuarioAutenticado.getId());
            System.out.println("Hash recibido: " + hash);
            System.out.println("Tamaño archivo: " + archivo.getSize() + " bytes");
            
            // Guardar archivo en subcarpeta del usuario (con hash corto en el nombre)
            rutaAlmacenamiento = fileStorageService.storeFileForUser(archivo, usuarioAutenticado.getId(), hash);
            System.out.println("Archivo guardado en: " + rutaAlmacenamiento);
            
            // Extraer nombre de archivo de la ruta
            String nombreArchivo = rutaAlmacenamiento.substring(rutaAlmacenamiento.lastIndexOf("/") + 1);
            
            // Crear entidad Documento
            Documento documento = new Documento();
            documento.setUsuario(usuarioAutenticado);
            documento.setNombreAlmacenado(nombreArchivo);
            documento.setNombreOriginal(nombreOriginalSanitizado);
            documento.setHashDocumento(hash);
            documento.setFechaFirma(fechaFirma);
            documento.setTipoContenido("application/pdf");
            documento.setTamano(archivo.getSize());
            documento.setRutaAlmacenamiento(rutaAlmacenamiento);
            documento.setFechaSubida(LocalDateTime.now());
            
            // Guardar en base de datos
            System.out.println("Guardando en base de datos...");
            Documento guardado = documentoService.CrearDocumentoFirmado(documento);
            System.out.println("Documento guardado con ID: " + guardado.getId());
            
            // Preparar respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", guardado.getId());
            respuesta.put("mensaje", "Documento guardado exitosamente");
            respuesta.put("nombreArchivo", guardado.getNombreAlmacenado());
            respuesta.put("fechaFirma", guardado.getFechaFirma().format(DateTimeFormatter.ISO_DATE_TIME));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
            
        } catch (RuntimeException e) {
            // Si falló después de guardar el archivo, intentar eliminarlo
            if (rutaAlmacenamiento != null) {
                try {
                    fileStorageService.deleteFileByRelativePath(rutaAlmacenamiento);
                    System.out.println("Archivo eliminado después de error: " + rutaAlmacenamiento);
                } catch (Exception ex) {
                    System.err.println("Error al eliminar archivo después de error: " + ex.getMessage());
                }
            }
            System.err.println("Error de autenticación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no autenticado: " + e.getMessage()));
        } catch (Exception e) {
            // Si falló después de guardar el archivo, intentar eliminarlo
            if (rutaAlmacenamiento != null) {
                try {
                    fileStorageService.deleteFileByRelativePath(rutaAlmacenamiento);
                    System.err.println("Archivo eliminado después de error: " + rutaAlmacenamiento);
                } catch (Exception ex) {
                    System.err.println("Error al eliminar archivo después de error: " + ex.getMessage());
                }
            }
            System.err.println("Error al guardar documento: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al guardar documento: " + e.getMessage()));
        }
    }

    /**
     * GET /api/documentos/firmados/usuario/{usuarioId} o GET /documentos/usuario/{usuarioId}
     * Obtiene todos los documentos firmados de un usuario
     * Compatible con ambas rutas para mantener compatibilidad con frontend
     */
    @GetMapping({"/firmados/usuario/{usuarioId}", "/usuario/{usuarioId}"})
    public ResponseEntity<?> obtenerDocumentosFirmadosPorUsuario(@PathVariable Long usuarioId) {
        try {
            // Validar autenticación
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            
            // Validar que el usuarioId en la URL coincida con el del token
            if (!usuarioAutenticado.getId().equals(usuarioId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para acceder a los documentos de otro usuario"));
            }
            
            // Obtener documentos del usuario
            List<Documento> documentos = documentoService.ObtenerDocumentosFirmadosPorUsuario(usuarioId);
            
            // Convertir a DTO
            List<Map<String, Object>> documentosDTO = documentos.stream()
                .map(this::toDTOFirmado)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(documentosDTO);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no autenticado: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al obtener documentos: " + e.getMessage()));
        }
    }

    /**
     * GET /api/documentos/{documentoId}/download-firmado
     * Descarga un documento firmado
     */
    @GetMapping("/{documentoId}/download-firmado")
    public ResponseEntity<?> descargarDocumentoFirmado(@PathVariable Long documentoId) {
        try {
            // Validar autenticación
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            
            // Obtener documento
            Documento documento = documentoService.ObtenerDocumentoPorId(documentoId);
            
            // Validar que el documento pertenezca al usuario autenticado
            if (!documento.getUsuario().getId().equals(usuarioAutenticado.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para descargar este documento"));
            }
            
            // Cargar archivo
            Resource resource;
            if (documento.getRutaAlmacenamiento() != null) {
                resource = fileStorageService.loadFileByRelativePath(documento.getRutaAlmacenamiento());
            } else {
                resource = fileStorageService.loadFileAsResource(documento.getNombreAlmacenado());
            }
            
            // Codificar nombre del archivo para el header
            String nombreArchivoCodificado = URLEncoder.encode(
                documento.getNombreOriginal(), 
                StandardCharsets.UTF_8
            );
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + documento.getNombreOriginal() + "\"; filename*=UTF-8''" + nombreArchivoCodificado)
                .body(resource);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al descargar documento: " + e.getMessage()));
        }
    }

    /**
     * Sanitiza el nombre de archivo para evitar problemas de seguridad
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "documento.pdf";
        
        // Remover caracteres peligrosos
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Limitar longitud
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        
        // Si está vacío después de sanitizar, usar nombre por defecto
        if (sanitized.trim().isEmpty()) {
            sanitized = "documento.pdf";
        }
        
        return sanitized;
    }

    private Map<String, Object> toDTOFirmado(Documento d) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", d.getId());
        dto.put("usuarioId", d.getUsuario() != null ? d.getUsuario().getId() : null);
        dto.put("nombre", d.getNombreAlmacenado());
        dto.put("nombreOriginal", d.getNombreOriginal());
        dto.put("hash", d.getHashDocumento());
        dto.put("fechaFirma", d.getFechaFirma() != null ? 
            d.getFechaFirma().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        dto.put("tipoArchivo", d.getTipoContenido());
        dto.put("tamaño", d.getTamano());
        return dto;
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