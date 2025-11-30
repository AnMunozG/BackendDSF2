package cl.veritrust.v1.Controller;

import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Service.ServicioService;
import cl.veritrust.v1.DTO.ServicioDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/servicios")
public class ServicioController {
    @Autowired
    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping
    public List<ServicioDTO> GetAllServicios() {
        return servicioService.ObtenerServicios().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ServicioDTO GetServicioById(@PathVariable Long id) {
        return toDTO(servicioService.ObtenerServicioPorId(id));
    }

    @PostMapping
    public ServicioDTO CreateServicio(@RequestBody ServicioDTO servicioDTO) {
        Servicio s = toEntity(servicioDTO);
        Servicio creado = servicioService.CrearServicio(s);
        return toDTO(creado);
    }

    @PutMapping("/{id}")
    public ServicioDTO UpdateServicio(@PathVariable Long id, @RequestBody ServicioDTO servicioDTO) {
        Servicio detalles = toEntity(servicioDTO);
        Servicio actualizado = servicioService.ActualizarServicio(id, detalles);
        return toDTO(actualizado);
    }

    @DeleteMapping("/{id}")
    public void DeleteServicio(@PathVariable Long id) {
        servicioService.EliminarServicio(id);
    }

    private ServicioDTO toDTO(Servicio s) {
        if (s == null) return null;
        ServicioDTO dto = new ServicioDTO();
        dto.setId(s.getId());
        dto.setNombre(s.getNombre());
        dto.setDescripcion(s.getDescripcion());
        dto.setPrecio(s.getPrecio());
        dto.setDetalles(s.getDetalles());
        return dto;
    }

    private Servicio toEntity(ServicioDTO dto) {
        if (dto == null) return null;
        Servicio s = new Servicio();
        s.setId(dto.getId());
        s.setNombre(dto.getNombre());
        s.setDescripcion(dto.getDescripcion());
        s.setPrecio(dto.getPrecio());
        s.setDetalles(dto.getDetalles());
        return s;
    }
}