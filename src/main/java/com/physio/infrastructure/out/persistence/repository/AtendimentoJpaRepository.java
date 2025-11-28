        package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT a FROM AtendimentoEntity a " +
            "WHERE a.dataHoraInicio BETWEEN :inicio AND :fim " +
            "AND a.servicoBase.id IN :servicoIds " +
            "AND a.recebedor IS NOT NULL " +
            "ORDER BY a.dataHoraInicio ASC")
    List<AtendimentoEntity> findParaRelatorioFinanceiro(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("servicoIds") List<Integer> servicoIds
    );

    // Buscar atendimentos concluídos no mês para dashboard
    @Query("SELECT COUNT(a) FROM AtendimentoEntity a " +
            "WHERE a.status = 'CONCLUIDO' " +
            "AND a.dataHoraInicio >= :inicio " +
            "AND a.dataHoraInicio < :fim")
    Long countConcluidosPorMes(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    // Buscar atendimentos avulsos concluídos com valor > 0 para cálculo de faturamento
    @Query("SELECT a FROM AtendimentoEntity a " +
            "WHERE a.status = 'CONCLUIDO' " +
            "AND a.valorCobrado > 0 " +
            "AND a.dataHoraInicio >= :inicio " +
            "AND a.dataHoraInicio < :fim")
    List<AtendimentoEntity> findAvulsosConcluidosPorMes(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}