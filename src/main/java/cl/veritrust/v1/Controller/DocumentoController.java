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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
@Tag(name = "Documentos", description = "API para gestión de documentos y firma digital")
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
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Operation(
        summary = "Subir documento",
        description = "Sube un documento (PDF o DOCX) al sistema y lo asocia a un usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documento subido exitosamente"),
        @ApiResponse(responseCode = "400", description = "Tipo de archivo no permitido o usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/upload")
    public ResponseEntity<DocumentoDTO> uploadFile(
            @Parameter(description = "Archivo a subir (PDF o DOCX)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID del usuario propietario del documento", required = true)
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
            fileStorageService.deleteFile(nombreAlmacenado);
            return ResponseEntity.badRequest().build();
        }

        Documento guardado = documentoService.CrearDocumento(doc);
        return ResponseEntity.ok(toDTO(guardado));
    }

    @Operation(
        summary = "Crear documento",
        description = "Crea un nuevo documento en el sistema mediante DTO"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Documento creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos del documento inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ResponseEntity<DocumentoDTO> createDocumento(
            @Parameter(description = "Datos del documento a crear", required = true)
            @Valid @RequestBody DocumentoDTO dto) {
        Documento d = toEntity(dto);
        Documento creado = documentoService.CrearDocumento(d);
        URI location = URI.create("/api/documentos/" + creado.getId());
        return ResponseEntity.created(location).body(toDTO(creado));
    }

    @Operation(
        summary = "Actualizar documento",
        description = "Actualiza la información de un documento existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documento actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{id}")
    public ResponseEntity<DocumentoDTO> updateDocumento(
            @Parameter(description = "ID del documento a actualizar", required = true)
            @PathVariable Long id,
            @Parameter(description = "Datos actualizados del documento", required = true)
            @Valid @RequestBody DocumentoDTO dto) {
        Documento detalles = toEntity(dto);
        Documento actualizado = documentoService.ActualizarDocumento(id, detalles);
        return ResponseEntity.ok(toDTO(actualizado));
    }

    @Operation(
        summary = "Firmar documento",
        description = "Firma un documento existente. No permite refirmar documentos ya firmados."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documento firmado exitosamente"),
        @ApiResponse(responseCode = "400", description = "El documento ya ha sido firmado previamente"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/firmar")
    public ResponseEntity<?> firmarDocumento(
            @Parameter(description = "ID del documento a firmar", required = true)
            @PathVariable Long id) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        
        if (doc.isFirmado()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("mensaje", "El documento ya ha sido firmado previamente."));
        }

        Documento actualizado = firmarDoc.signDocumento(doc);
        return ResponseEntity.ok(toDTO(actualizado));
    }

    @Operation(
        summary = "Descargar documento",
        description = "Descarga un documento del sistema mediante su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documento descargado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "ID del documento a descargar", required = true)
            @PathVariable Long id) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        Resource resource = fileStorageService.loadFileAsResource(doc.getNombreAlmacenado());
        String archivoCodificado = URLEncoder.encode(doc.getNombreOriginal(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + archivoCodificado)
                .body(resource);
    }

    @Operation(
        summary = "Listar documentos por usuario",
        description = "Obtiene todos los documentos asociados a un usuario específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de documentos obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentoDTO>> listByUser(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable Long userId) {
        List<Documento> filtered = documentoService.ObtenerDocumentosPorUsuario(userId);
        List<DocumentoDTO> dtos = filtered.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(
        summary = "Eliminar documento",
        description = "Elimina un documento del sistema y sus archivos asociados (original y firmado si existen)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Documento eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocumento(
            @Parameter(description = "ID del documento a eliminar", required = true)
            @PathVariable Long id) {
        Documento doc = documentoService.ObtenerDocumentoPorId(id);
        
        fileStorageService.deleteFile(doc.getNombreAlmacenado());

        if (doc.getNombreFirmado() != null) {
            fileStorageService.deleteFile(doc.getNombreFirmado());
        }
        if (doc.getRutaAlmacenamiento() != null) {
            fileStorageService.deleteFileByRelativePath(doc.getRutaAlmacenamiento());
        }

        documentoService.EliminarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Guardar documento firmado",
        description = "Guarda un documento que ha sido firmado desde el frontend. Requiere autenticación y valida que el usuario tenga permisos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Documento firmado guardado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (archivo, hash, nombre, fecha)"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/firmado")
    public ResponseEntity<?> guardarDocumentoFirmado(
            @Parameter(description = "Archivo PDF firmado", required = true)
            @RequestParam("archivo") MultipartFile archivo,
            @Parameter(description = "Nombre original del archivo", required = true)
            @RequestParam("nombreOriginal") String nombreOriginal,
            @Parameter(description = "Hash SHA-256 del documento (64 caracteres)", required = true)
            @RequestParam("hash") String hash,
            @Parameter(description = "Fecha de firma en formato ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam("fechaFirma") String fechaFirmaStr,
            @Parameter(description = "Tipo de archivo (por defecto: pdf)")
            @RequestParam(value = "tipoArchivo", required = false, defaultValue = "pdf") String tipoArchivo) {
        
        String rutaAlmacenamiento = null;
        
        try {
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            
            if (archivo == null || archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo es requerido"));
            }
            
            String contentType = archivo.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Solo se permiten archivos PDF"));
            }
            
            if (archivo.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo excede el tamaño máximo de 10MB"));
            }
            
            if (hash == null || hash.trim().isEmpty() || hash.length() != 64) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El hash SHA-256 es requerido y debe tener 64 caracteres"));
            }
            
            if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El nombre original del archivo es requerido"));
            }
            
            LocalDateTime fechaFirma;
            try {
                fechaFirma = LocalDateTime.parse(fechaFirmaStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de fecha inválido. Use formato ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)"));
            }
            
            String nombreOriginalSanitizado = sanitizeFileName(nombreOriginal);
            
            if (tipoArchivo == null || tipoArchivo.trim().isEmpty()) {
                tipoArchivo = "pdf";
            }
            
            rutaAlmacenamiento = fileStorageService.storeFileForUser(archivo, usuarioAutenticado.getId(), hash);
            
            String nombreArchivo = rutaAlmacenamiento.substring(rutaAlmacenamiento.lastIndexOf("/") + 1);
            
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
            
            Documento guardado = documentoService.CrearDocumentoFirmado(documento);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", guardado.getId());
            respuesta.put("mensaje", "Documento guardado exitosamente");
            respuesta.put("nombreArchivo", guardado.getNombreAlmacenado());
            respuesta.put("fechaFirma", guardado.getFechaFirma().format(DateTimeFormatter.ISO_DATE_TIME));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
            
        } catch (RuntimeException e) {
            if (rutaAlmacenamiento != null) {
                try {
                    fileStorageService.deleteFileByRelativePath(rutaAlmacenamiento);
                } catch (Exception ex) {
                    System.err.println("Error al eliminar archivo después de error: " + ex.getMessage());
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no autenticado: " + e.getMessage()));
        } catch (Exception e) {
            if (rutaAlmacenamiento != null) {
                try {
                    fileStorageService.deleteFileByRelativePath(rutaAlmacenamiento);
                } catch (Exception ex) {
                    System.err.println("Error al eliminar archivo después de error: " + ex.getMessage());
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al guardar documento: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Obtener documentos firmados por usuario",
        description = "Obtiene todos los documentos firmados de un usuario específico. El usuario autenticado solo puede ver sus propios documentos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de documentos firmados obtenida exitosamente"),
        @ApiResponse(responseCode = "403", description = "No tienes permiso para acceder a los documentos de otro usuario"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping({"/firmados/usuario/{usuarioId}", "/usuario/{usuarioId}"})
    public ResponseEntity<?> obtenerDocumentosFirmadosPorUsuario(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable Long usuarioId) {
        try {
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            
            if (!usuarioAutenticado.getId().equals(usuarioId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para acceder a los documentos de otro usuario"));
            }
            
            List<Documento> documentos = documentoService.ObtenerDocumentosFirmadosPorUsuario(usuarioId);
            
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

    @Operation(
        summary = "Descargar documento firmado",
        description = "Descarga un documento que ha sido firmado. El usuario solo puede descargar sus propios documentos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documento firmado descargado exitosamente"),
        @ApiResponse(responseCode = "403", description = "No tienes permiso para descargar este documento"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{documentoId}/download-firmado")
    public ResponseEntity<?> descargarDocumentoFirmado(
            @Parameter(description = "ID del documento firmado a descargar", required = true)
            @PathVariable Long documentoId) {
        try {
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            
            Documento documento = documentoService.ObtenerDocumentoPorId(documentoId);
            
            if (!documento.getUsuario().getId().equals(usuarioAutenticado.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para descargar este documento"));
            }
            
            Resource resource;
            if (documento.getRutaAlmacenamiento() != null) {
                resource = fileStorageService.loadFileByRelativePath(documento.getRutaAlmacenamiento());
            } else {
                resource = fileStorageService.loadFileAsResource(documento.getNombreAlmacenado());
            }
            
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al descargar documento: " + e.getMessage()));
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "documento.pdf";
        
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        
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