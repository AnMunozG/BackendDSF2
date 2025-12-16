package cl.veritrust.v1.Service;
import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ServicioService {
    @Autowired
    private ServicioRepository servicioRepository;

    @Transactional(readOnly = true)
    public List<Servicio> ObtenerServicios() {
        return servicioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Servicio ObtenerServicioPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + id));
    }

    public Servicio CrearServicio(Servicio servicio) {
        return servicioRepository.save(servicio);
    }

    public Servicio ActualizarServicio(Long id, Servicio detallesServicio) {
        Servicio servicio = ObtenerServicioPorId(id);
        
        servicio.setNombre(detallesServicio.getNombre());
        servicio.setDescripcion(detallesServicio.getDescripcion());
        servicio.setPrecio(detallesServicio.getPrecio());
        servicio.setDescripcionCompleta(detallesServicio.getDescripcionCompleta()); 

        if (detallesServicio.getDetalles() != null) {
             servicio.setDetalles(detallesServicio.getDetalles());
        }

        return servicioRepository.save(servicio);
    }

    public void EliminarServicio(Long id) {
        Servicio servicio = ObtenerServicioPorId(id);
        servicioRepository.delete(servicio);
    }
}