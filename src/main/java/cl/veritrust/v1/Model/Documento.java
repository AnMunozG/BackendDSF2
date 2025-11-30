package cl.veritrust.v1.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreOriginal;

    @Column(nullable = false, unique = true)
    private String nombreAlmacenado;

    private String tipoContenido;

    private Long tamano;

    private LocalDateTime fechaSubida;

    private boolean firmado = false;

    private String nombreFirmado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}