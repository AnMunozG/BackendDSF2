package cl.veritrust.v1.Service;

import cl.veritrust.v1.Exception.ResourceNotFoundException;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Usuario> ObtenerUsuarios() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario ObtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    // Método auxiliar para buscar por RUT
    @Transactional(readOnly = true)
    public Usuario ObtenerUsuarioPorRut(String rut) {
        return usuarioRepository.findByRut(rut)
                .orElse(null);
    }

    public Usuario CrearUsuario(Usuario usuario) {
        if (usuario.getRol() == null) {
            usuario.setRol("user");
        }
        // Encriptamos la contraseña antes de guardar
        usuario.setContraseña(passwordEncoder.encode(usuario.getContraseña()));
        return usuarioRepository.save(usuario);
    }

    public Usuario ActualizarUsuario(Long id, Usuario detallesUsuario) {
        Usuario usuario = ObtenerUsuarioPorId(id);
        usuario.setRut(detallesUsuario.getRut());
        usuario.setNombre(detallesUsuario.getNombre());
        usuario.setTelefono(detallesUsuario.getTelefono());
        usuario.setEmail(detallesUsuario.getEmail());
        usuario.setFechaNac(detallesUsuario.getFechaNac());
        
        // Solo encriptamos si la contraseña ha cambiado y no viene vacía
        if (detallesUsuario.getContraseña() != null && !detallesUsuario.getContraseña().isEmpty()) {
            usuario.setContraseña(passwordEncoder.encode(detallesUsuario.getContraseña()));
        }
        
        if (detallesUsuario.getRol() != null) {
            usuario.setRol(detallesUsuario.getRol());
        }
        return usuarioRepository.save(usuario);
    }

    public void EliminarUsuario(Long id) {
        Usuario usuario = ObtenerUsuarioPorId(id);
        usuarioRepository.delete(usuario);
    }

    // Este método es usado internamente por Spring Security para el Login
    @Override
    public UserDetails loadUserByUsername(String rut) throws UsernameNotFoundException {
        Usuario u = ObtenerUsuarioPorRut(rut);
        if (u == null) {
            throw new UsernameNotFoundException("Usuario no encontrado con RUT: " + rut);
        }
        // Mapeamos tu Usuario a un usuario de Spring Security
        return new org.springframework.security.core.userdetails.User(
                u.getRut(), 
                u.getContraseña(), 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + u.getRol()))
        );
    }

    // Método antiguo de Login (mantenido por si acaso, pero el controller usará el nuevo)
    public Usuario Login(String rut, String contraseña) {
        return null; 
    }
}