package cl.veritrust.v1.Controller;

import org.springframework.http.ResponseEntity; 
import org.springframework.web.bind.annotation.RestController;
import cl.veritrust.v1.Service.UsuarioService;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.DTO.UsuarioDTO;
import cl.veritrust.v1.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    public UsuarioController() { }

    @GetMapping
    public List<UsuarioDTO> GetAllUsuarios() {
        return usuarioService.ObtenerUsuarios().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UsuarioDTO GetUsuarioById(@PathVariable Long id) {
        return toDTO(usuarioService.ObtenerUsuarioPorId(id));
    }

    @PostMapping
    public UsuarioDTO CreateUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        Usuario usuario = toEntity(usuarioDTO);
        Usuario creado = usuarioService.CrearUsuario(usuario);
        return toDTO(creado);
    }

    @PutMapping("/{id}")
    public UsuarioDTO UpdateUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuarioDTO) {
        Usuario detalles = toEntity(usuarioDTO);
        Usuario actualizado = usuarioService.ActualizarUsuario(id, detalles);
        return toDTO(actualizado);
    }

    @DeleteMapping("/{id}")
    public void DeleteUsuario(@PathVariable Long id) {
        usuarioService.EliminarUsuario(id);
    }

    // --- NUEVO LOGIN SEGURO CON JWT ---
    @PostMapping("/login")
    public ResponseEntity<?> Login(@RequestBody UsuarioDTO usuarioDTO) {
        try {
            // 1. Spring Security verifica las credenciales (RUT y password encriptada)
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usuarioDTO.getRut(), usuarioDTO.getContraseña())
            );

            // 2. Si pasa, buscamos los datos completos del usuario
            Usuario u = usuarioService.ObtenerUsuarioPorRut(usuarioDTO.getRut());

            // 3. Generamos el token JWT
            String token = jwtUtil.generateToken(u.getRut(), u.getRol());

            // 4. Devolvemos Token + Datos del usuario al frontend
            Map<String, Object> response = Map.of(
                "token", token,
                "usuario", toDTO(u)
            );
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("mensaje", "Credenciales incorrectas"));
        }
    }
    
    private UsuarioDTO toDTO(Usuario u) {
        if (u == null) return null;
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setRut(u.getRut());
        dto.setNombre(u.getNombre());
        dto.setTelefono(u.getTelefono());
        dto.setEmail(u.getEmail());
        dto.setFechaNac(u.getFechaNac());
        // No devolvemos contraseña
        dto.setRol(u.getRol());
        return dto;
    }

    private Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) return null;
        Usuario u = new Usuario();
        u.setId(dto.getId());
        u.setRut(dto.getRut());
        u.setNombre(dto.getNombre());
        u.setTelefono(dto.getTelefono());
        u.setEmail(dto.getEmail());
        u.setFechaNac(dto.getFechaNac());
        u.setContraseña(dto.getContraseña());
        u.setRol(dto.getRol());
        return u;
    }
}