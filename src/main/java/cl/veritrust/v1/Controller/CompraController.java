package cl.veritrust.v1.Controller;

import cl.veritrust.v1.DTO.CompraDTO;
import cl.veritrust.v1.Model.Compra;
import cl.veritrust.v1.Model.Servicio;
import cl.veritrust.v1.Model.Usuario;
import cl.veritrust.v1.Security.SecurityUtil;
import cl.veritrust.v1.Service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/compras")
@Tag(name = "Compras", description = "API para gestión de compras de servicios")
public class CompraController {

	@Autowired
	private CompraService compraService;
	
	@Autowired
	private SecurityUtil securityUtil;

	@Operation(
		summary = "Obtener todas las compras",
		description = "Retorna una lista con todas las compras registradas en el sistema"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Lista de compras obtenida exitosamente"),
		@ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping
	public List<CompraDTO> getAll() {
		return compraService.ObtenerCompras().stream().map(this::toDTO).collect(Collectors.toList());
	}

	@Operation(
		summary = "Obtener compra por ID",
		description = "Retorna la información de una compra específica mediante su ID"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Compra encontrada"),
		@ApiResponse(responseCode = "404", description = "Compra no encontrada"),
		@ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping("/{id}")
	public ResponseEntity<CompraDTO> getById(
			@Parameter(description = "ID de la compra a buscar", required = true)
			@PathVariable Long id) {
		Compra c = compraService.ObtenerCompraPorId(id);
		return ResponseEntity.ok(toDTO(c));
	}

	@Operation(
		summary = "Obtener compras por usuario",
		description = "Retorna todas las compras realizadas por un usuario específico. El usuario autenticado solo puede ver sus propias compras."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Lista de compras del usuario obtenida exitosamente"),
		@ApiResponse(responseCode = "403", description = "No tienes permiso para acceder a las compras de otro usuario"),
		@ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping("/usuario/{usuarioId}")
	public ResponseEntity<?> getComprasPorUsuario(
			@Parameter(description = "ID del usuario", required = true)
			@PathVariable Long usuarioId) {
		try {
			// Validar que el usuario esté autenticado
			Usuario usuarioAutenticado = securityUtil.getUsuarioAutenticado();
			
			// Validar que el usuarioId en la URL coincida con el del token
			if (!usuarioAutenticado.getId().equals(usuarioId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("error", "No tienes permiso para acceder a las compras de otro usuario"));
			}
			
			// Obtener compras del usuario con información del servicio
			// El JOIN FETCH carga las relaciones dentro de la transacción
			List<Compra> compras = compraService.ObtenerComprasPorUsuario(usuarioId);
			
			// Convertir a DTO con información del servicio
			// Acceder a las relaciones mientras están cargadas
			List<Map<String, Object>> comprasDTO = compras.stream()
				.map(this::toDTOConServicio)
				.collect(Collectors.toList());
			
			return ResponseEntity.ok(comprasDTO);
			
		} catch (RuntimeException e) {
			System.err.println("Error de autenticación en getComprasPorUsuario: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("error", "Usuario no autenticado: " + e.getMessage()));
		} catch (Exception e) {
			System.err.println("Error al obtener compras: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Error al obtener compras: " + e.getMessage()));
		}
	}

	@Operation(
		summary = "Crear nueva compra",
		description = "Registra una nueva compra en el sistema. Acepta datos flexibles (string o número) para usuarioId, servicioId y monto."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Compra creada exitosamente"),
		@ApiResponse(responseCode = "400", description = "Datos de compra inválidos"),
		@ApiResponse(responseCode = "500", description = "Error interno del servidor"),
		@ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping
	public ResponseEntity<?> create(
			@Parameter(description = "Datos de la compra (usuarioId, servicioId, monto opcional, fechaCompra opcional)", required = true)
			@RequestBody Map<String, Object> rawData) {
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

	@Operation(
		summary = "Actualizar compra",
		description = "Actualiza la información de una compra existente"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Compra actualizada exitosamente"),
		@ApiResponse(responseCode = "404", description = "Compra no encontrada"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping("/{id}")
	public ResponseEntity<CompraDTO> update(
			@Parameter(description = "ID de la compra a actualizar", required = true)
			@PathVariable Long id,
			@Parameter(description = "Datos actualizados de la compra", required = true)
			@Valid @RequestBody CompraDTO dto) {
		Compra detalles = toEntity(dto);
		Compra actualizado = compraService.ActualizarCompra(id, detalles);
		return ResponseEntity.ok(toDTO(actualizado));
	}

	@Operation(
		summary = "Eliminar compra",
		description = "Elimina una compra del sistema mediante su ID"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Compra eliminada exitosamente"),
		@ApiResponse(responseCode = "404", description = "Compra no encontrada"),
		@ApiResponse(responseCode = "401", description = "No autorizado - Token JWT inválido o faltante")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "ID de la compra a eliminar", required = true)
			@PathVariable Long id) {
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
	
	// Mapeo con información del servicio para el endpoint de usuario
	private Map<String, Object> toDTOConServicio(Compra c) {
		if (c == null) return null;
		
		try {
			Map<String, Object> compraMap = new HashMap<>();
			compraMap.put("id", c.getId());
			
			// Acceder a usuario de forma segura
			Usuario usuario = c.getUsuario();
			compraMap.put("usuarioId", usuario != null ? usuario.getId() : null);
			
			// Acceder a servicio de forma segura (debe estar cargado por JOIN FETCH)
			Servicio servicio = c.getServicio();
			compraMap.put("servicioId", servicio != null ? servicio.getId() : null);
			
			// Formatear fecha como LocalDate (solo fecha, sin hora)
			if (c.getFechaCompra() != null) {
				compraMap.put("fechaCompra", c.getFechaCompra().toLocalDate().toString());
			} else {
				compraMap.put("fechaCompra", null);
			}
			
			compraMap.put("monto", c.getMonto());
			
			// Información del servicio
			Map<String, Object> servicioMap = new HashMap<>();
			if (servicio != null) {
				servicioMap.put("id", servicio.getId());
				servicioMap.put("nombre", servicio.getNombre() != null ? servicio.getNombre() : "");
				servicioMap.put("precio", servicio.getPrecio());
			}
			compraMap.put("servicio", servicioMap);
			
			return compraMap;
		} catch (Exception e) {
			System.err.println("Error al convertir compra a DTO: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Error al procesar compra: " + e.getMessage(), e);
		}
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
