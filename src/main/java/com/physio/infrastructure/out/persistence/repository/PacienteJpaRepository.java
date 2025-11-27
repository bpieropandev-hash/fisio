package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.PacienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteJpaRepository extends JpaRepository<PacienteEntity, Integer> {
    Optional<PacienteEntity> findById(Integer id);
    Optional<PacienteEntity> findByCpf(String cpf);
    List<PacienteEntity> findByAtivoTrue();
}
