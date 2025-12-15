package cl.veritrust.v1.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "documentos_firmados")
public class DocumentoFirmado {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull
    private Usuario usuario;
    
    @NotBlank
    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;
    
    @NotBlank
    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;
    
    @NotBlank
    @Column(name = "hash_documento", nullable = false, length = 64)
    private String hashDocumento;
    
    @NotNull
    @Column(name = "fecha_firma", nullable = false)
    private LocalDateTime fechaFirma;
    
    @Column(name = "tipo_archivo", length = 50)
    private String tipoArchivo;
    
    @Column(name = "tamaño")
    private Long tamaño;
    
    @NotBlank
    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;
}
