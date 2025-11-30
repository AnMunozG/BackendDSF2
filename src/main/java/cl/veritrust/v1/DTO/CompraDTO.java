package cl.veritrust.v1.DTO;

public class CompraDTO {
	private Long id;
	private Long usuarioId;
	private Long servicioId;
	private String fechaCompra;
	private Integer monto;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(Long usuarioId) {
		this.usuarioId = usuarioId;
	}

	public Long getServicioId() {
		return servicioId;
	}

	public void setServicioId(Long servicioId) {
		this.servicioId = servicioId;
	}

	public String getFechaCompra() {
		return fechaCompra;
	}

	public void setFechaCompra(String fechaCompra) {
		this.fechaCompra = fechaCompra;
	}

	public Integer getMonto() {
		return monto;
	}

	public void setMonto(Integer monto) {
		this.monto = monto;
	}
}
