package cl.veritrust.v1.Service;
import cl.veritrust.v1.Exception.ResourceNotFoundException;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Usuario> ObtenerUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario ObtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    public Usuario CrearUsuario(Usuario usuario) {
        if (usuario.getRol() == null) {
            usuario.setRol("Usuario");
        }
        return usuarioRepository.save(usuario);
    }

    public Usuario ActualizarUsuario(Long id, Usuario detallesUsuario) {
        Usuario usuario = ObtenerUsuarioPorId(id);
        usuario.setRut(detallesUsuario.getRut());
        usuario.setNombre(detallesUsuario.getNombre());
        usuario.setTelefono(detallesUsuario.getTelefono());
        usuario.setEmail(detallesUsuario.getEmail());
        usuario.setFechaNac(detallesUsuario.getFechaNac());
        usuario.setContraseña(detallesUsuario.getContraseña());
        if (detallesUsuario.getRol() != null) {
            usuario.setRol(detallesUsuario.getRol());
        }
        return usuarioRepository.save(usuario);
    }

    public void EliminarUsuario(Long id) {
        Usuario usuario = ObtenerUsuarioPorId(id);
        usuarioRepository.delete(usuario);
    }

    public Usuario Login(String rut, String contraseña) {
        return usuarioRepository.findByRutAndContraseña(rut, contraseña)
                .orElseThrow(() -> new ResourceNotFoundException("Credenciales inválidas"));
    }

}
