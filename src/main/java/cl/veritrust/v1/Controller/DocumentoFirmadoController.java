package cl.veritrust.v1.Controller;

import cl.veritrust.v1.Exception.ResourceNotFoundException;
import cl.veritrust.v1.Model.DocumentoFirmado;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Security.SecurityUtil;
import cl.veritrust.v1.Service.DocumentoFirmadoService;
import cl.veritrust.v1.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@RequestMapping("/documentos")
public class DocumentoFirmadoController {
    
    @Autowired
    private DocumentoFirmadoService documentoFirmadoService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    /**
     * POST /documentos
     * Guarda un documento firmado
     */
    @PostMapping
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
            
            // Crear entidad DocumentoFirmado
            DocumentoFirmado documentoFirmado = new DocumentoFirmado();
            documentoFirmado.setUsuario(usuarioAutenticado);
            documentoFirmado.setNombreArchivo(nombreArchivo);
            documentoFirmado.setNombreOriginal(nombreOriginalSanitizado);
            documentoFirmado.setHashDocumento(hash);
            documentoFirmado.setFechaFirma(fechaFirma);
            documentoFirmado.setTipoArchivo(tipoArchivo);
            documentoFirmado.setTamaño(archivo.getSize());
            documentoFirmado.setRutaAlmacenamiento(rutaAlmacenamiento);
            
            // Guardar en base de datos
            System.out.println("Guardando en base de datos...");
            DocumentoFirmado guardado = documentoFirmadoService.crearDocumentoFirmado(documentoFirmado);
            System.out.println("Documento guardado con ID: " + guardado.getId());
            
            // Preparar respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", guardado.getId());
            respuesta.put("mensaje", "Documento guardado exitosamente");
            respuesta.put("nombreArchivo", guardado.getNombreArchivo());
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
     * GET /documentos/usuario/{usuarioId}
     * Obtiene todos los documentos firmados de un usuario
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> obtenerDocumentosPorUsuario(@PathVariable Long usuarioId) {
        try {
            // Validar autenticación
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            
            // Validar que el usuarioId en la URL coincida con el del token
            if (!usuarioAutenticado.getId().equals(usuarioId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para acceder a los documentos de otro usuario"));
            }
            
            // Obtener documentos del usuario
            List<DocumentoFirmado> documentos = documentoFirmadoService.obtenerDocumentosFirmadosPorUsuario(usuarioId);
            
            // Convertir a DTO
            List<Map<String, Object>> documentosDTO = documentos.stream()
                .map(this::toDTO)
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
     * GET /documentos/{documentoId}/download
     * Descarga un documento firmado
     */
    @GetMapping("/{documentoId}/download")
    public ResponseEntity<?> descargarDocumento(@PathVariable Long documentoId) {
        try {
            // Validar autenticación
            Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
            
            // Obtener documento
            DocumentoFirmado documento = documentoFirmadoService.obtenerDocumentoFirmadoPorId(documentoId);
            
            // Validar que el documento pertenezca al usuario autenticado
            if (!documento.getUsuario().getId().equals(usuarioAutenticado.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para descargar este documento"));
            }
            
            // Cargar archivo
            Resource resource = fileStorageService.loadFileByRelativePath(documento.getRutaAlmacenamiento());
            
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
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Documento no encontrado"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no autenticado: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al descargar documento: " + e.getMessage()));
        }
    }
    
    // Métodos auxiliares
    
    private Map<String, Object> toDTO(DocumentoFirmado d) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", d.getId());
        dto.put("usuarioId", d.getUsuario() != null ? d.getUsuario().getId() : null);
        dto.put("nombre", d.getNombreArchivo());
        dto.put("nombreOriginal", d.getNombreOriginal());
        dto.put("hash", d.getHashDocumento());
        dto.put("fechaFirma", d.getFechaFirma() != null ? 
            d.getFechaFirma().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        dto.put("tipoArchivo", d.getTipoArchivo());
        dto.put("tamaño", d.getTamaño());
        return dto;
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
}
