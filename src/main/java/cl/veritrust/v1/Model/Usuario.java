package cl.veritrust.v1.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long id;
    
    @NotBlank
    @Column(unique = true, nullable = false)
    private String rut;
    
    @NotBlank
    @Column(nullable = false)
    private String nombre;
    
    private String telefono;
    
    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;
    
    @NotNull
    @Column(nullable = false)
    private LocalDate fechaNac;
    
    @NotBlank
    @Column(nullable = false)
    private String contraseña;
    
    @Column(nullable = false)
    private String rol = "user";
}