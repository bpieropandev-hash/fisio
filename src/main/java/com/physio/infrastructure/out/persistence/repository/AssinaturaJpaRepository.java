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

    // Buscar todas as assinaturas ativas. JOIN FETCH é obrigatório aqui (não opcional
    // como em endpoints web com open-in-view): quem chama isso é GeradorCobrancaService,
    // acionado por @Scheduled/ApplicationReadyEvent — sem request HTTP, então sem sessão
    // aberta por open-in-view. Sem o fetch, acessar assinatura.paciente/servico fora do
    // repositório explode com LazyInitializationException (achado rodando o catch-up no boot).
    @Query("SELECT a FROM AssinaturaEntity a " +
           "JOIN FETCH a.paciente " +
           "JOIN FETCH a.servico " +
           "WHERE a.ativo = true")
    List<AssinaturaEntity> findByAtivoTrue();

    // Buscar assinaturas de um paciente
    List<AssinaturaEntity> findByPaciente_Id(Integer pacienteId);

    // Buscar assinaturas ativas de um serviço específico (para reajuste com propagação)
    List<AssinaturaEntity> findByServico_IdAndAtivoTrue(Integer servicoId);
}

