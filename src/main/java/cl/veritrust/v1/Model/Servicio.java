package cl.veritrust.v1.Model;

import jakarta.persistence.*; // Asegúrate de importar todo esto
import lombok.Data;
import java.util.List;

@Data
@Entity
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    
    private String descripcion;
    
    private Integer precio;

    // --- AQUI ESTA EL ARREGLO ---
    @ElementCollection // <--- ESTO ES OBLIGATORIO PARA LISTAS
    @CollectionTable(name = "servicio_detalles", joinColumns = @JoinColumn(name = "servicio_id"))
    @Column(name = "detalle")
    private List<String> detalles;
}