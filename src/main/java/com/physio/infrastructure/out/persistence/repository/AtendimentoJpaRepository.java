package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT COUNT(a) > 0 FROM AtendimentoEntity a WHERE " +
            "(a.dataHoraInicio < :fim AND a.dataHoraFim > :inicio) " +
            "AND a.status <> 'CANCELADO'")
    boolean existsConflitoDeHorario(LocalDateTime inicio, LocalDateTime fim);
}