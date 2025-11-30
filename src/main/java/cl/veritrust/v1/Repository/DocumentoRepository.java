package cl.veritrust.v1.Repository;
import cl.veritrust.v1.Model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
	// consulta para obtener documentos por usuario (usa el campo usuario.id)
	java.util.List<cl.veritrust.v1.Model.Documento> findByUsuarioId(Long usuarioId);
}