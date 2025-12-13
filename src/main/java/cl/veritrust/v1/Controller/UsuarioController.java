package cl.veritrust.v1.Controller;

import cl.veritrust.v1.DTO.UsuarioDTO;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Security.JwtUtil;
import cl.veritrust.v1.Service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

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

    // ---------------- CRUD ----------------

    @GetMapping
    public List<UsuarioDTO> getAllUsuarios() {
        return usuarioService.ObtenerUsuarios()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UsuarioDTO getUsuarioById(@PathVariable Long id) {
        return toDTO(usuarioService.ObtenerUsuarioPorId(id));
    }

    @PostMapping
    public UsuarioDTO createUsuario(@Valid @RequestBody UsuarioDTO dto) {
        Usuario creado = usuarioService.CrearUsuario(toEntity(dto));
        return toDTO(creado);
    }

    @PutMapping("/{id}")
    public UsuarioDTO updateUsuario(@PathVariable Long id,
                                    @Valid @RequestBody UsuarioDTO dto) {
        Usuario actualizado =
                usuarioService.ActualizarUsuario(id, toEntity(dto));
        return toDTO(actualizado);
    }

    @DeleteMapping("/{id}")
    public void deleteUsuario(@PathVariable Long id) {
        usuarioService.EliminarUsuario(id);
    }

    // ---------------- LOGIN JWT ----------------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioDTO dto) {
        try {
            // 1️⃣ Autenticación REAL (usa PasswordEncoder internamente)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getRut(),
                            dto.getContraseña()
                    )
            );

            // 2️⃣ Si pasa, obtenemos el usuario
            Usuario usuario =
                    usuarioService.ObtenerUsuarioPorRut(dto.getRut());

            // 3️⃣ Generamos JWT
            String token = jwtUtil.generateToken(
                    usuario.getRut(),
                    usuario.getRol()
            );

            // 4️⃣ Respuesta al frontend
            return ResponseEntity.ok(
                    Map.of(
                            "token", token,
                            "usuario", toDTO(usuario)
                    )
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of("mensaje", "Credenciales incorrectas"));
        }
    }

    // ---------------- MAPPERS ----------------

    private UsuarioDTO toDTO(Usuario u) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setRut(u.getRut());
        dto.setNombre(u.getNombre());
        dto.setTelefono(u.getTelefono());
        dto.setEmail(u.getEmail());
        dto.setFechaNac(u.getFechaNac());
        dto.setRol(u.getRol());
        return dto;
    }

    private Usuario toEntity(UsuarioDTO dto) {
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
