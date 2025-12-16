package cl.veritrust.v1.DTO;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class DocumentoDTO {

    private Long id;

    @NotBlank(message = "El nombre original no puede estar vacío")
    private String nombreOriginal;

    @NotBlank(message = "El nombre almacenado no puede estar vacío")
    private String nombreAlmacenado;

    @NotBlank(message = "El tipo de contenido no puede estar vacío")
    private String tipoContenido;

    @NotNull(message = "El tamaño no puede ser nulo")
    private Long tamano;

    @NotNull(message = "La fecha de subida no puede ser nula")
    private LocalDateTime fechaSubida;

    private boolean firmado;

    private String nombreFirmado;

    @NotNull(message = "El ID del usuario no puede ser nulo")
    private Long usuarioId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(String nombreAlmacenado) { this.nombreAlmacenado = nombreAlmacenado; }

    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }

    public Long getTamano() { return tamano; }
    public void setTamano(Long tamano) { this.tamano = tamano; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public boolean isFirmado() { return firmado; }
    public void setFirmado(boolean firmado) { this.firmado = firmado; }

    public String getNombreFirmado() { return nombreFirmado; }
    public void setNombreFirmado(String nombreFirmado) { this.nombreFirmado = nombreFirmado; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
}
