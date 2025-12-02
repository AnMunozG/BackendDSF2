package cl.veritrust.v1.Service;
import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServicioService {
    @Autowired
    private ServicioRepository servicioRepository;

    public List<Servicio> ObtenerServicios() {
        return servicioRepository.findAll();
    }

    public Servicio ObtenerServicioPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + id));
    }

    public Servicio CrearServicio(Servicio servicio) {
        return servicioRepository.save(servicio);
    }

    // --- LÓGICA DE ACTUALIZACIÓN (PUT) CORREGIDA ---
    public Servicio ActualizarServicio(Long id, Servicio detallesServicio) {
        Servicio servicio = ObtenerServicioPorId(id);
        
        // Copia de los campos básicos
        servicio.setNombre(detallesServicio.getNombre());
        servicio.setDescripcion(detallesServicio.getDescripcion());
        servicio.setPrecio(detallesServicio.getPrecio());
        
        // 🚨 COPIAR LOS CAMPOS NUEVOS/LARGOS 🚨
        servicio.setDescripcionCompleta(detallesServicio.getDescripcionCompleta()); 

        // Copiar la lista de detalles (ARRAY)
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