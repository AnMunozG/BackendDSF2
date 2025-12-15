package cl.veritrust.v1.Repository;

import cl.veritrust.v1.Model.DocumentoFirmado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoFirmadoRepository extends JpaRepository<DocumentoFirmado, Long> {
    
    List<DocumentoFirmado> findByUsuario_IdOrderByFechaFirmaDesc(Long usuarioId);
    
    @Query("SELECT d FROM DocumentoFirmado d WHERE d.usuario.id = :usuarioId ORDER BY d.fechaFirma DESC")
    List<DocumentoFirmado> findDocumentosFirmadosPorUsuario(@Param("usuarioId") Long usuarioId);
}
