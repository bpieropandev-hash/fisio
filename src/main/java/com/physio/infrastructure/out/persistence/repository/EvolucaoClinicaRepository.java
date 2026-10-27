package com.physio.infrastructure.out.persistence.repository;
import com.physio.infrastructure.out.persistence.entity.EvolucaoClinicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EvolucaoClinicaRepository extends JpaRepository<EvolucaoClinicaEntity,Long> {
    Optional<EvolucaoClinicaEntity> findFirstByAtendimentoIdOrderByVersaoDesc(Integer atendimentoId);
    List<EvolucaoClinicaEntity> findByAtendimentoIdOrderByVersaoDesc(Integer atendimentoId);
}
