package cl.veritrust.v1.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class DocumentoFirmadoDTO {
    
    private Long id;
    
    @NotNull(message = "El ID del usuario no puede ser nulo")
    private Long usuarioId;
    
    @NotBlank(message = "El nombre del archivo no puede estar vacío")
    private String nombreArchivo;
    
    @NotBlank(message = "El nombre original no puede estar vacío")
    private String nombreOriginal;
    
    @NotBlank(message = "El hash del documento no puede estar vacío")
    private String hash;
    
    @NotNull(message = "La fecha de firma no puede ser nula")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaFirma;
    
    private String tipoArchivo;
    
    private Long tamaño;
    
    private String rutaAlmacenamiento;
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    
    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }
    
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    
    public LocalDateTime getFechaFirma() { return fechaFirma; }
    public void setFechaFirma(LocalDateTime fechaFirma) { this.fechaFirma = fechaFirma; }
    
    public String getTipoArchivo() { return tipoArchivo; }
    public void setTipoArchivo(String tipoArchivo) { this.tipoArchivo = tipoArchivo; }
    
    public Long getTamaño() { return tamaño; }
    public void setTamaño(Long tamaño) { this.tamaño = tamaño; }
    
    public String getRutaAlmacenamiento() { return rutaAlmacenamiento; }
    public void setRutaAlmacenamiento(String rutaAlmacenamiento) { this.rutaAlmacenamiento = rutaAlmacenamiento; }
}
