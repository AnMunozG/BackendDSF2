package cl.veritrust.v1.Security;

import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    
    @Autowired
    private UsuarioService usuarioService;
    
    /**
     * Obtiene el usuario autenticado desde el contexto de seguridad
     * @return Usuario autenticado
     * @throws RuntimeException si no hay usuario autenticado
     */
    public Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        String rut = null;
        
        if (principal instanceof UserDetails) {
            rut = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            rut = (String) principal;
        } else {
            throw new RuntimeException("No se pudo obtener el RUT del usuario autenticado");
        }
        
        Usuario usuario = usuarioService.ObtenerUsuarioPorRut(rut);
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado con RUT: " + rut);
        }
        
        return usuario;
    }
    
    /**
     * Obtiene el RUT del usuario autenticado
     * @return RUT del usuario
     */
    public String getRutUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        } else {
            throw new RuntimeException("No se pudo obtener el RUT del usuario autenticado");
        }
    }
}
