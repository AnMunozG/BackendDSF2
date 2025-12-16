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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "API para gestión de usuarios y autenticación")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    // ---------------- CRUD ----------------

    @Operation(
        summary = "Obtener todos los usuarios",
        description = "Retorna una lista con todos los usuarios registrados en el sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public List<UsuarioDTO> getAllUsuarios() {
        return usuarioService.ObtenerUsuarios()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(
        summary = "Obtener usuario por ID",
        description = "Retorna la información de un usuario específico mediante su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    public UsuarioDTO getUsuarioById(
            @Parameter(description = "ID del usuario a buscar", required = true)
            @PathVariable Long id) {
        return toDTO(usuarioService.ObtenerUsuarioPorId(id));
    }

    @Operation(
        summary = "Crear nuevo usuario",
        description = "Registra un nuevo usuario en el sistema. Este endpoint es público y no requiere autenticación."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de usuario inválidos")
    })
    @PostMapping
    public UsuarioDTO createUsuario(
            @Parameter(description = "Datos del usuario a crear", required = true)
            @Valid @RequestBody UsuarioDTO dto) {
        Usuario creado = usuarioService.CrearUsuario(toEntity(dto));
        return toDTO(creado);
    }

    @Operation(
        summary = "Actualizar usuario",
        description = "Actualiza la información de un usuario existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{id}")
    public UsuarioDTO updateUsuario(
            @Parameter(description = "ID del usuario a actualizar", required = true)
            @PathVariable Long id,
            @Parameter(description = "Datos actualizados del usuario", required = true)
            @Valid @RequestBody UsuarioDTO dto) {
        Usuario actualizado =
                usuarioService.ActualizarUsuario(id, toEntity(dto));
        return toDTO(actualizado);
    }

    @Operation(
        summary = "Eliminar usuario",
        description = "Elimina un usuario del sistema mediante su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    public void deleteUsuario(
            @Parameter(description = "ID del usuario a eliminar", required = true)
            @PathVariable Long id) {
        usuarioService.EliminarUsuario(id);
    }

    // ---------------- LOGIN JWT ----------------

    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica un usuario y retorna un token JWT. Este endpoint es público y no requiere autenticación."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login exitoso - Retorna token JWT y datos del usuario"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Parameter(description = "Credenciales de acceso (RUT y contraseña)", required = true)
            @RequestBody UsuarioDTO dto) {
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
