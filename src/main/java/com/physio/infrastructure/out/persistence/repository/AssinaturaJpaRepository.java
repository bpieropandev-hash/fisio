package com.physio.infrastructure.out.persistence.repository;

import com.physio.infrastructure.out.persistence.entity.AssinaturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssinaturaJpaRepository extends JpaRepository<AssinaturaEntity, Integer> {

    // Buscar assinaturas ativas de um paciente para um serviço específico
    @Query("SELECT a FROM AssinaturaEntity a " +
           "WHERE a.paciente.id = :pacienteId " +
           "AND a.servico.id = :servicoId " +
           "AND a.ativo = true")
    Optional<AssinaturaEntity> findAtivaPorPacienteEServico(
            @Param("pacienteId") Integer pacienteId,
            @Param("servicoId") Integer servicoId
    );

    // Buscar todas as assinaturas ativas
    List<AssinaturaEntity> findByAtivoTrue();

    // Buscar assinaturas de um paciente
    List<AssinaturaEntity> findByPaciente_Id(Integer pacienteId);
}

