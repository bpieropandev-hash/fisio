package com.physio.infrastructure.out.persistence.repository;
import com.physio.infrastructure.out.persistence.entity.ReciboEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ReciboJpaRepository extends JpaRepository<ReciboEntity,Long> {
 Optional<ReciboEntity> findByAtendimentoId(Integer id);
 Optional<ReciboEntity> findByCobrancaId(Integer id);
}
