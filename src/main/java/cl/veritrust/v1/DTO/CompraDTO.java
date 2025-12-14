package cl.veritrust.v1.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class CompraDTO {

    private Long id;

    @NotNull(message = "El ID del usuario no puede ser nulo")
    private Long usuarioId;

    @NotNull(message = "El ID del servicio no puede ser nulo")
    private Long servicioId;

    // La fecha es opcional - si no se proporciona, el servicio asignará la fecha actual
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime fechaCompra;

    // El monto es opcional - si no se proporciona, se calculará desde el precio del servicio
    @Min(value = 0, message = "El monto no puede ser negativo")
    private Integer monto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    
    public Long getServicioId() { return servicioId; }
    public void setServicioId(Long servicioId) { this.servicioId = servicioId; }
    public LocalDateTime getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(LocalDateTime fechaCompra) { this.fechaCompra = fechaCompra; }
    public Integer getMonto() { return monto; }
    public void setMonto(Integer monto) { this.monto = monto; }
}
