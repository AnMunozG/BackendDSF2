package cl.veritrust.v1.Controller;

import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Service.ServicioService;
import cl.veritrust.v1.DTO.ServicioDTO;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/servicios")
@Tag(name = "Servicios", description = "API para gestión de servicios ofrecidos")
public class ServicioController {
    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @Operation(
        summary = "Obtener todos los servicios",
        description = "Retorna una lista con todos los servicios disponibles. Este endpoint es público."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de servicios obtenida exitosamente")
    })
    @GetMapping
    public List<ServicioDTO> GetAllServicios() {
        return servicioService.ObtenerServicios().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Operation(
        summary = "Obtener servicio por ID",
        description = "Retorna la información de un servicio específico mediante su ID. Este endpoint es público."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Servicio encontrado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    @GetMapping("/{id}")
    public ServicioDTO GetServicioById(
            @Parameter(description = "ID del servicio a buscar", required = true)
            @PathVariable Long id) {
        return toDTO(servicioService.ObtenerServicioPorId(id));
    }

    @Operation(
        summary = "Crear nuevo servicio",
        description = "Registra un nuevo servicio en el sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Servicio creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos del servicio inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ServicioDTO CreateServicio(
            @Parameter(description = "Datos del servicio a crear", required = true)
            @RequestBody ServicioDTO servicioDTO) {
        Servicio s = toEntity(servicioDTO);
        Servicio creado = servicioService.CrearServicio(s);
        return toDTO(creado);
    }

    @Operation(
        summary = "Actualizar servicio",
        description = "Actualiza la información de un servicio existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Servicio actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{id}")
    public ServicioDTO UpdateServicio(
            @Parameter(description = "ID del servicio a actualizar", required = true)
            @PathVariable Long id,
            @Parameter(description = "Datos actualizados del servicio", required = true)
            @RequestBody ServicioDTO servicioDTO) {
        Servicio detalles = toEntity(servicioDTO);
        Servicio actualizado = servicioService.ActualizarServicio(id, detalles);
        return toDTO(actualizado);
    }

    @Operation(
        summary = "Eliminar servicio",
        description = "Elimina un servicio del sistema mediante su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Servicio eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    public void DeleteServicio(
            @Parameter(description = "ID del servicio a eliminar", required = true)
            @PathVariable Long id) {
        servicioService.EliminarServicio(id);
    }
    
    // --- MAPEO DE ENTIDAD A DTO ---
    private ServicioDTO toDTO(Servicio s) {
        if (s == null) return null;
        ServicioDTO dto = new ServicioDTO();
        dto.setId(s.getId());
        dto.setNombre(s.getNombre());
        dto.setDescripcion(s.getDescripcion());
        dto.setPrecio(s.getPrecio());
        dto.setDetalles(s.getDetalles());
        // 🚨 CORRECCIÓN 1: Aseguramos que el DTO también lea la descripción completa del Entity
        dto.setDescripcionCompleta(s.getDescripcionCompleta()); 
        return dto;
    }

    // --- MAPEO DE DTO A ENTIDAD (CRÍTICO PARA GUARDAR) ---
    private Servicio toEntity(ServicioDTO dto) {
        if (dto == null) return null;
        Servicio s = new Servicio();
        s.setId(dto.getId());
        s.setNombre(dto.getNombre());
        s.setDescripcion(dto.getDescripcion());
        s.setPrecio(dto.getPrecio());
        s.setDetalles(dto.getDetalles());
        // 🚨 CORRECCIÓN 2: COPIAR EL CAMPO NUEVO DEL EDITOR PARA QUE EL SERVICE LO PUEDA GUARDAR
        s.setDescripcionCompleta(dto.getDescripcionCompleta());
        return s;
    }
}