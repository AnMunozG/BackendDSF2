package cl.veritrust.v1.Controller;

import cl.veritrust.v1.DTO.CompraDTO;
import cl.veritrust.v1.Model.Compra;
import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/compras")
public class CompraController {

	@Autowired
	private CompraService compraService;

	@GetMapping
	public List<CompraDTO> getAll() {
		return compraService.ObtenerCompras().stream().map(this::toDTO).collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CompraDTO> getById(@PathVariable Long id) {
		Compra c = compraService.ObtenerCompraPorId(id);
		return ResponseEntity.ok(toDTO(c));
	}

	@PostMapping
	public ResponseEntity<CompraDTO> create(@Valid @RequestBody CompraDTO dto) {
		Compra c = toEntity(dto);
		Compra creado = compraService.CrearCompra(c);
		URI location = URI.create("/compras/" + creado.getId());
		return ResponseEntity.created(location).body(toDTO(creado));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CompraDTO> update(@PathVariable Long id, @Valid @RequestBody CompraDTO dto) {
		Compra detalles = toEntity(dto);
		Compra actualizado = compraService.ActualizarCompra(id, detalles);
		return ResponseEntity.ok(toDTO(actualizado));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		compraService.EliminarCompra(id);
		return ResponseEntity.noContent().build();
	}

	// Mapeos simples
	private CompraDTO toDTO(Compra c) {
		if (c == null) return null;
		CompraDTO dto = new CompraDTO();
		dto.setId(c.getId());
		if (c.getUsuario() != null) dto.setUsuarioId(c.getUsuario().getId());
		if (c.getServicio() != null) dto.setServicioId(c.getServicio().getId());
		dto.setFechaCompra(c.getFechaCompra());
		dto.setMonto(c.getMonto());
		return dto;
	}

	private Compra toEntity(CompraDTO dto) {
		if (dto == null) return null;
		Compra c = new Compra();
		c.setId(dto.getId());
		if (dto.getUsuarioId() != null) {
			Usuario u = new Usuario();
			u.setId(dto.getUsuarioId());
			c.setUsuario(u);
		}
		if (dto.getServicioId() != null) {
			Servicio s = new Servicio();
			s.setId(dto.getServicioId());
			c.setServicio(s);
		}
		c.setFechaCompra(dto.getFechaCompra());
		c.setMonto(dto.getMonto());
		return c;
	}
}
