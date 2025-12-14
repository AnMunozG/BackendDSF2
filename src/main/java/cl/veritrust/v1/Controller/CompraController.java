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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
	public ResponseEntity<?> create(@RequestBody Map<String, Object> rawData) {
		try {
			// Validar que el cuerpo no esté vacío
			if (rawData == null || rawData.isEmpty()) {
				return ResponseEntity.badRequest()
					.body(Map.of("error", "El cuerpo de la petición no puede estar vacío"));
			}
			
			// Crear DTO y convertir los datos de forma flexible
			CompraDTO dto = new CompraDTO();
			
			// Convertir usuarioId (acepta string o número)
			Object usuarioIdObj = rawData.get("usuarioId");
			if (usuarioIdObj == null) {
				return ResponseEntity.badRequest()
					.body(Map.of("error", "El ID del usuario es requerido"));
			}
			dto.setUsuarioId(convertToLong(usuarioIdObj));
			
			// Convertir servicioId (acepta string o número)
			Object servicioIdObj = rawData.get("servicioId");
			if (servicioIdObj == null) {
				return ResponseEntity.badRequest()
					.body(Map.of("error", "El ID del servicio es requerido"));
			}
			dto.setServicioId(convertToLong(servicioIdObj));
			
			// Convertir monto (opcional, acepta string o número)
			Object montoObj = rawData.get("monto");
			if (montoObj != null) {
				Integer monto = convertToInteger(montoObj);
				if (monto != null && monto < 0) {
					return ResponseEntity.badRequest()
						.body(Map.of("error", "El monto no puede ser negativo"));
				}
				dto.setMonto(monto);
			}
			
			// Convertir fecha (opcional, acepta múltiples formatos)
			Object fechaObj = rawData.get("fechaCompra");
			if (fechaObj != null) {
				LocalDateTime fecha = parseFlexibleDate(fechaObj.toString());
				dto.setFechaCompra(fecha);
			}
			
			Compra c = toEntity(dto);
			Compra creado = compraService.CrearCompra(c);
			URI location = URI.create("/compras/" + creado.getId());
			return ResponseEntity.created(location).body(toDTO(creado));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500)
				.body(Map.of(
					"error", "Error al crear la compra: " + e.getMessage(),
					"tipo", e.getClass().getSimpleName(),
					"causa", e.getCause() != null ? e.getCause().getMessage() : "Sin causa"
				));
		}
	}
	
	// Métodos auxiliares para conversión flexible
	private Long convertToLong(Object value) {
		if (value == null) return null;
		if (value instanceof Long) return (Long) value;
		if (value instanceof Integer) return ((Integer) value).longValue();
		if (value instanceof String) {
			try {
				return Long.parseLong((String) value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("El ID debe ser un número válido: " + value);
			}
		}
		throw new IllegalArgumentException("Tipo no válido para ID: " + value.getClass().getSimpleName());
	}
	
	private Integer convertToInteger(Object value) {
		if (value == null) return null;
		if (value instanceof Integer) return (Integer) value;
		if (value instanceof Long) return ((Long) value).intValue();
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
	
	// Método para parsear fechas en múltiples formatos
	private LocalDateTime parseFlexibleDate(String dateStr) {
		if (dateStr == null || dateStr.trim().isEmpty()) return null;
		
		dateStr = dateStr.trim();
		java.time.format.DateTimeFormatter[] formats = {
			java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
			java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
			java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
			java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
		};
		
		for (java.time.format.DateTimeFormatter formatter : formats) {
			try {
				if (formatter.equals(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) ||
					formatter.equals(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) ||
					formatter.equals(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))) {
					java.time.LocalDate date = java.time.LocalDate.parse(dateStr, formatter);
					return date.atStartOfDay();
				}
				return LocalDateTime.parse(dateStr, formatter);
			} catch (Exception e) {
				// Continuar con el siguiente formato
			}
		}
		return null; // Si no se puede parsear, retornar null y el servicio asignará la fecha actual
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
