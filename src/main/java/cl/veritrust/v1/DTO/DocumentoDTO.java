package cl.veritrust.v1.DTO;

import java.time.LocalDateTime;

public class DocumentoDTO {
	private Long id;
	private String nombreOriginal;
	private String nombreAlmacenado;
	private String tipoContenido;
	private Long tamano;
	private LocalDateTime fechaSubida;
	private boolean firmado;
	private String nombreFirmado;
	private Long usuarioId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombreOriginal() {
		return nombreOriginal;
	}

	public void setNombreOriginal(String nombreOriginal) {
		this.nombreOriginal = nombreOriginal;
	}

	public String getNombreAlmacenado() {
		return nombreAlmacenado;
	}

	public void setNombreAlmacenado(String nombreAlmacenado) {
		this.nombreAlmacenado = nombreAlmacenado;
	}

	public String getTipoContenido() {
		return tipoContenido;
	}

	public void setTipoContenido(String tipoContenido) {
		this.tipoContenido = tipoContenido;
	}

	public Long getTamano() {
		return tamano;
	}

	public void setTamano(Long tamano) {
		this.tamano = tamano;
	}

	public LocalDateTime getFechaSubida() {
		return fechaSubida;
	}

	public void setFechaSubida(LocalDateTime fechaSubida) {
		this.fechaSubida = fechaSubida;
	}

	public boolean isFirmado() {
		return firmado;
	}

	public void setFirmado(boolean firmado) {
		this.firmado = firmado;
	}

	public String getNombreFirmado() {
		return nombreFirmado;
	}

	public void setNombreFirmado(String nombreFirmado) {
		this.nombreFirmado = nombreFirmado;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(Long usuarioId) {
		this.usuarioId = usuarioId;
	}
}
