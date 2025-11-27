package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtendimentoJpaRepository extends JpaRepository<AtendimentoEntity, Long> {
}

