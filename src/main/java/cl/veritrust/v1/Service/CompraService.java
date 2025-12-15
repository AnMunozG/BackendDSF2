package cl.veritrust.v1.Service;

import cl.veritrust.v1.Exception.ResourceNotFoundException;
import cl.veritrust.v1.Model.Compra;
import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CompraService {

	@Autowired
	private CompraRepository compraRepository;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private ServicioService servicioService;

	@Transactional(readOnly = true)
	public List<Compra> ObtenerCompras() {
		return compraRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Compra ObtenerCompraPorId(Long id) {
		return compraRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
	}

	@Transactional(readOnly = true)
	public List<Compra> ObtenerComprasPorUsuario(Long usuarioId) {
		List<Compra> compras = compraRepository.findComprasPorUsuarioConServicio(usuarioId);
		// Forzar la inicialización de las relaciones dentro de la transacción
		// Esto evita problemas de lazy loading fuera de la transacción
		for (Compra compra : compras) {
			if (compra.getServicio() != null) {
				// Acceder a los campos para forzar la carga
				compra.getServicio().getId();
				compra.getServicio().getNombre();
				compra.getServicio().getPrecio();
			}
			if (compra.getUsuario() != null) {
				compra.getUsuario().getId();
			}
		}
		return compras;
	}

	public Compra CrearCompra(Compra compra) {
		// Validar que el monto sea positivo
		if (compra.getMonto() == null || compra.getMonto() < 0) {
			throw new IllegalArgumentException("El monto debe ser un valor positivo");
		}
		
		// Resolver relaciones si se pasan solo los ids en las referencias
		if (compra.getUsuario() != null && compra.getUsuario().getId() != null) {
			Usuario usuario = usuarioService.ObtenerUsuarioPorId(compra.getUsuario().getId());
			compra.setUsuario(usuario);
		} else {
			throw new IllegalArgumentException("El usuario es requerido");
		}
		
		if (compra.getServicio() != null && compra.getServicio().getId() != null) {
			Servicio servicio = servicioService.ObtenerServicioPorId(compra.getServicio().getId());
			compra.setServicio(servicio);
		} else {
			throw new IllegalArgumentException("El servicio es requerido");
		}
		
		// Si no tiene fecha, asignar la fecha actual
		if (compra.getFechaCompra() == null) {
			compra.setFechaCompra(LocalDateTime.now());
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
		if (detallesCompra.getFechaCompra() != null) {
			compra.setFechaCompra(detallesCompra.getFechaCompra());
		}
		if (detallesCompra.getMonto() != null) {
			if (detallesCompra.getMonto() < 0) {
				throw new IllegalArgumentException("El monto no puede ser negativo");
			}
			compra.setMonto(detallesCompra.getMonto());
		}
		return compraRepository.save(compra);
	}

	public void EliminarCompra(Long id) {
		Compra compra = ObtenerCompraPorId(id);
		compraRepository.delete(compra);
	}
}
