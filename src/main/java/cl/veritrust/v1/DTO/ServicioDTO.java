package cl.veritrust.v1.DTO;

import jakarta.validation.constraints.*;
import java.util.List;

public class ServicioDTO {

    private Long id;

    @NotBlank(message = "El nombre del servicio no puede estar vacío")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(min = 10, max = 255, message = "La descripción debe tener entre 10 y 255 caracteres")
    private String descripcion;

    @NotNull(message = "El precio no puede ser nulo")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private Integer precio;

    @NotNull(message = "La lista de detalles no puede ser nula")
    @Size(min = 1, message = "Debe existir al menos un detalle")
    private List<
        @NotBlank(message = "El detalle no puede estar vacío")
        @Size(max = 255, message = "Cada detalle debe tener como máximo 255 caracteres")
        String
    > detalles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }
    public List<String> getDetalles() { return detalles; }
    public void setDetalles(List<String> detalles) { this.detalles = detalles; }
}
