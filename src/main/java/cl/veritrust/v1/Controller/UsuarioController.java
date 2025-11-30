package cl.veritrust.v1.Controller;

import org.springframework.web.bind.annotation.RestController;
import cl.veritrust.v1.Service.UsuarioService;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.DTO.UsuarioDTO;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

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

    @PostMapping("/login")
    public UsuarioDTO Login(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        Usuario u = usuarioService.Login(usuarioDTO.getRut(), usuarioDTO.getContraseña());
        return toDTO(u);
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
        dto.setContraseña(u.getContraseña());
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