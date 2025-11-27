package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.ServicoConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicoConfigJpaRepository extends JpaRepository<ServicoConfigEntity, Integer> {
    Optional<ServicoConfigEntity> findByIdAndAtivoTrue(Integer id);
    Optional<ServicoConfigEntity> findByNome(String nome);
    List<ServicoConfigEntity> findByAtivoTrue();
}
