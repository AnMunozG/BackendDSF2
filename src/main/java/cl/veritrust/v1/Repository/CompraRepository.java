package cl.veritrust.v1.Repository;

import cl.veritrust.v1.Model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    
    @Query("SELECT DISTINCT c FROM Compra c JOIN FETCH c.servicio JOIN FETCH c.usuario WHERE c.usuario.id = :usuarioId ORDER BY c.fechaCompra DESC")
    List<Compra> findComprasPorUsuarioConServicio(@Param("usuarioId") Long usuarioId);
    
    List<Compra> findByUsuario_IdOrderByFechaCompraDesc(Long usuarioId);
}
