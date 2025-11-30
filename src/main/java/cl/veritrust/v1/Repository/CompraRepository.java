package cl.veritrust.v1.Repository;

import cl.veritrust.v1.Model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {    
}
