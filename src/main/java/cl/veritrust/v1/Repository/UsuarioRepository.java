package cl.veritrust.v1.Repository;

import cl.veritrust.v1.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByRutAndContraseña(String rut, String contraseña);
}
