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
        return usuarioRepository.save(usuario);
    }

    public void EliminarUsuario(Long id) {
        Usuario usuario = ObtenerUsuarioPorId(id);
        usuarioRepository.delete(usuario);
    }

    public Usuario Login(String email, String contraseña) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario usuario : usuarios) {
            if (usuario.getEmail().equals(email) && usuario.getContraseña().equals(contraseña)) {
                return usuario;
            }
        }
        throw new ResourceNotFoundException("Credenciales inválidas");
    }

}
