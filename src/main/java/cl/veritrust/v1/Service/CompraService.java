package cl.veritrust.v1.Service;

import cl.veritrust.v1.Exception.ResourceNotFoundException;
import cl.veritrust.v1.Model.Compra;
import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompraService {

	@Autowired
	private CompraRepository compraRepository;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private ServicioService servicioService;

	public List<Compra> ObtenerCompras() {
		return compraRepository.findAll();
	}

	public Compra ObtenerCompraPorId(Long id) {
		return compraRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
	}

	public Compra CrearCompra(Compra compra) {
		// Resolver relaciones si se pasan solo los ids en las referencias
		if (compra.getUsuario() != null && compra.getUsuario().getId() != null) {
			Usuario usuario = usuarioService.ObtenerUsuarioPorId(compra.getUsuario().getId());
			compra.setUsuario(usuario);
		}
		if (compra.getServicio() != null && compra.getServicio().getId() != null) {
			Servicio servicio = servicioService.ObtenerServicioPorId(compra.getServicio().getId());
			compra.setServicio(servicio);
		}
		return compraRepository.save(compra);
	}

	public Compra ActualizarCompra(Long id, Compra detallesCompra) {
		Compra compra = ObtenerCompraPorId(id);
		if (detallesCompra.getUsuario() != null && detallesCompra.getUsuario().getId() != null) {
			Usuario usuario = usuarioService.ObtenerUsuarioPorId(detallesCompra.getUsuario().getId());
			compra.setUsuario(usuario);
		}
		if (detallesCompra.getServicio() != null && detallesCompra.getServicio().getId() != null) {
			Servicio servicio = servicioService.ObtenerServicioPorId(detallesCompra.getServicio().getId());
			compra.setServicio(servicio);
		}
		if (detallesCompra.getFechaCompra() != null) compra.setFechaCompra(detallesCompra.getFechaCompra());
		if (detallesCompra.getMonto() != null) compra.setMonto(detallesCompra.getMonto());
		return compraRepository.save(compra);
	}

	public void EliminarCompra(Long id) {
		Compra compra = ObtenerCompraPorId(id);
		compraRepository.delete(compra);
	}
}
