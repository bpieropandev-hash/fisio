package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
// Mudei Long para Integer aqui pois sua Entity usa Integer no ID
public interface AtendimentoJpaRepository extends JpaRepository<AtendimentoEntity, Integer> {

    // Busca para a Agenda e Financeiro
    List<AtendimentoEntity> findByDataHoraInicioBetween(LocalDateTime inicio, LocalDateTime fim);

    // Busca para o Prontuário
    List<AtendimentoEntity> findByPaciente_Id(Integer pacienteId);
}