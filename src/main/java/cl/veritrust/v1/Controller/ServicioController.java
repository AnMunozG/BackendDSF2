package cl.veritrust.v1.Controller;

import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Service.ServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/servicios")
@CrossOrigin(origins = "http://localhost:5173") // <--- ESTO FALTA
public class ServicioController {
    @Autowired
    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping
    public List<Servicio> GetAllServicios() {
        return servicioService.ObtenerServicios();
    }

    @GetMapping("/{id}")
    public Servicio GetServicioById(@PathVariable Long id) {
        return servicioService.ObtenerServicioPorId(id);
    }

    @PostMapping
    public Servicio CreateServicio(@RequestBody Servicio servicio) {
        return servicioService.CrearServicio(servicio);
    }

    @PutMapping("/{id}")
    public Servicio UpdateServicio(@PathVariable Long id, @RequestBody Servicio servicio) {
        return servicioService.ActualizarServicio(id, servicio);
    }

    @DeleteMapping("/{id}")
    public void DeleteServicio(@PathVariable Long id) {
        servicioService.EliminarServicio(id);
    }
}