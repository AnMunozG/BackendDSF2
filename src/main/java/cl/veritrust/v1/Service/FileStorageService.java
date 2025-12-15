package cl.veritrust.v1.Service;

import cl.veritrust.v1.Exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
 
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;
 
@Service
public class FileStorageService {
 
    private final Path uploadDir;
 
    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (Exception ex) {
            throw new FileStorageException("No se pudo crear la carpeta de uploads.", ex);
        }
    }
 
    public String storeFile(MultipartFile file) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (originalName.contains("..")) {
                throw new FileStorageException("Nombre de archivo inválido: " + originalName);
            }
            // prevenir colisiones: prefijo UUID
            String fileName = UUID.randomUUID().toString() + "_" + originalName;
            Path targetLocation = this.uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("No se pudo guardar el archivo " + originalName, ex);
        }
    }
    
    /**
     * Guarda un archivo en una subcarpeta específica del usuario
     * @param file Archivo a guardar
     * @param usuarioId ID del usuario para crear subcarpeta
     * @param hash Hash SHA-256 del documento (se usará un hash corto para el nombre)
     * @return Ruta relativa del archivo guardado (desde uploadDir)
     */
    public String storeFileForUser(MultipartFile file, Long usuarioId, String hash) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (originalName.contains("..")) {
                throw new FileStorageException("Nombre de archivo inválido: " + originalName);
            }
            
            // Crear subcarpeta para el usuario
            Path userDir = this.uploadDir.resolve("documentos").resolve(usuarioId.toString());
            Files.createDirectories(userDir);
            
            // Generar nombre de archivo con timestamp, usuarioId y hash corto (primeros 8 caracteres)
            long timestamp = System.currentTimeMillis();
            String hashCorto = (hash != null && hash.length() >= 8) ? hash.substring(0, 8).toUpperCase() : "UNKNOWN";
            String fileName = "DOCUMENTO_FIRMADO_" + timestamp + "_" + usuarioId + "_" + hashCorto + ".pdf";
            
            Path targetLocation = userDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Retornar ruta relativa desde uploadDir
            return "documentos/" + usuarioId + "/" + fileName;
        } catch (IOException ex) {
            throw new FileStorageException("No se pudo guardar el archivo " + originalName, ex);
        }
    }
    
    /**
     * Carga un archivo desde una ruta relativa
     * @param relativePath Ruta relativa desde uploadDir (ej: "documentos/123/archivo.pdf")
     * @return Resource del archivo
     */
    public Resource loadFileByRelativePath(String relativePath) {
        try {
            Path filePath = this.uploadDir.resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) return resource;
            else throw new FileStorageException("Archivo no encontrado " + relativePath);
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Archivo no encontrado " + relativePath, ex);
        }
    }
 
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) return resource;
            else throw new FileStorageException("Archivo no encontrado " + fileName);
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Archivo no encontrado " + fileName, ex);
        }
    }
 
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            return false;
        }
    }
    
    /**
     * Elimina un archivo usando su ruta relativa
     * @param relativePath Ruta relativa desde uploadDir
     * @return true si se eliminó correctamente
     */
    public boolean deleteFileByRelativePath(String relativePath) {
        try {
            Path filePath = this.uploadDir.resolve(relativePath).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            return false;
        }
    }

    // getter para que componentes externos (p.e. FirmarDoc) puedan resolver rutas
    public Path getUploadDir() {
        return this.uploadDir;
    }
}
