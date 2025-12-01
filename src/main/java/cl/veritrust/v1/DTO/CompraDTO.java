package cl.veritrust.v1.DTO;

import jakarta.validation.constraints.*;

public class CompraDTO {

    private Long id;

    @NotNull(message = "El ID del usuario no puede ser nulo")
    private Long usuarioId;

    @NotNull(message = "El ID del servicio no puede ser nulo")
    private Long servicioId;

    @NotBlank(message = "La fecha de compra no puede estar vacía")
    @Pattern(
        regexp = "\\d{2}-\\d{2}-\\d{4}",
        message = "La fecha debe tener el formato DD-MM-YYYY"
    )
    private String fechaCompra;

    @NotNull(message = "El monto no puede ser nulo")
    @Min(value = 0, message = "El monto no puede ser negativo")
    private Integer monto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getServicioId() { return servicioId; }
    public void setServicioId(Long servicioId) { this.servicioId = servicioId; }
    public String getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(String fechaCompra) { this.fechaCompra = fechaCompra; }
    public Integer getMonto() { return monto; }
    public void setMonto(Integer monto) { this.monto = monto; }
}
