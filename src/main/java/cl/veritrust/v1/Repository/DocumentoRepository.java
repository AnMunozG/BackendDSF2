package cl.veritrust.v1.Repository;
import cl.veritrust.v1.Model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
	// consulta para obtener documentos por usuario (usa la relación usuario.id)
	List<Documento> findByUsuario_Id(Long usuarioId);
}