package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.ConfiguracoesEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ConfiguracoesRepository extends JpaRepository<ConfiguracoesEntity,Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracoesEntity c where c.id = 1")
    Optional<ConfiguracoesEntity> bloquear();
}
