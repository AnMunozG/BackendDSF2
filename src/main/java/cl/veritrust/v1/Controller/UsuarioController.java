package cl.veritrust.v1.Controller;

import org.springframework.web.bind.annotation.RestController;
import cl.veritrust.v1.Service.UsuarioService;
import cl.veritrust.v1.Model.Usuario;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "http://localhost:5173") // <--- ESTO ES LA MAGIA QUE FALTA
public class UsuarioController {
    
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> GeatAllUsuarios() {
        return usuarioService.ObtenerUsuarios();
    }

    @GetMapping("/{id}")
    public Usuario GetUsuarioById(@PathVariable Long id) {
        return usuarioService.ObtenerUsuarioPorId(id);
    }

    @PostMapping
    public Usuario CreateUsuario(@RequestBody Usuario usuario) {
        return usuarioService.CrearUsuario(usuario);
    }

    @PutMapping("/{id}")
    public Usuario UpdateUsuario(@PathVariable Long id, @RequestBody Usuario usuario) {
        return usuarioService.ActualizarUsuario(id, usuario);
    }

    @DeleteMapping("/{id}") 
    public void DeleteUsuario(@PathVariable Long id) {
        usuarioService.EliminarUsuario(id);
    }

    @PostMapping("/login")
    public Usuario Login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String contraseña = credentials.get("contraseña");
        return usuarioService.Login(email, contraseña);
    }
}