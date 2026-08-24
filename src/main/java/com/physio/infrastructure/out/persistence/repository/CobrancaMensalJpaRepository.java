package com.physio.infrastructure.out.persistence.repository;

import com.physio.domain.model.StatusCobranca;
import com.physio.infrastructure.out.persistence.entity.CobrancaMensalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaMensalJpaRepository extends JpaRepository<CobrancaMensalEntity, Integer> {

    // Verificar se já existe cobrança para uma assinatura em um mês/ano específico.
    // JOIN FETCH obrigatório: GeradorCobrancaService chama isso via @Scheduled/boot,
    // sem sessão aberta por open-in-view (só existe em request HTTP) — o mapper mapeia
    // a assinatura inteira (incl. paciente/servico) mesmo quando o chamador só quer
    // checar existência, então sem fetch explode com LazyInitializationException.
    @Query("SELECT c FROM CobrancaMensalEntity c " +
           "JOIN FETCH c.assinatura a " +
           "JOIN FETCH a.paciente " +
           "JOIN FETCH a.servico " +
           "WHERE a.id = :assinaturaId " +
           "AND c.mesReferencia = :mesReferencia " +
           "AND c.anoReferencia = :anoReferencia")
    Optional<CobrancaMensalEntity> findByAssinatura_IdAndMesReferenciaAndAnoReferencia(
            @Param("assinaturaId") Integer assinaturaId,
            @Param("mesReferencia") Integer mesReferencia,
            @Param("anoReferencia") Integer anoReferencia
    );

    // Buscar cobranças pagas para relatório financeiro
    @Query("SELECT c FROM CobrancaMensalEntity c " +
           "WHERE c.status = :status " +
           "AND c.anoReferencia = :ano " +
           "AND c.mesReferencia = :mes " +
           "AND c.recebedor IS NOT NULL " +
           "ORDER BY c.dataPagamento ASC")
    List<CobrancaMensalEntity> findParaRelatorioFinanceiro(
            @Param("status") StatusCobranca status,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano
    );

    // Buscar cobranças pagas em um período (para relatório consolidado)
    @Query("SELECT c FROM CobrancaMensalEntity c " +
           "WHERE c.status = 'PAGO' " +
           "AND c.recebedor IS NOT NULL " +
           "AND (c.anoReferencia > :anoInicio OR (c.anoReferencia = :anoInicio AND c.mesReferencia >= :mesInicio)) " +
           "AND (c.anoReferencia < :anoFim OR (c.anoReferencia = :anoFim AND c.mesReferencia <= :mesFim)) " +
           "ORDER BY c.anoReferencia ASC, c.mesReferencia ASC, c.dataPagamento ASC")
    List<CobrancaMensalEntity> findPagasPorPeriodo(
            @Param("anoInicio") Integer anoInicio,
            @Param("mesInicio") Integer mesInicio,
            @Param("anoFim") Integer anoFim,
            @Param("mesFim") Integer mesFim
    );

    // Faturamento da profissional com mensalidades (P2.3): soma em banco em vez de carregar
    // entidades pra somar em Java. Mesmo filtro de findPagasPorPeriodo.
    @Query("SELECT COALESCE(SUM(c.valor * c.pctProfissionalSnapshot / 100), 0) FROM CobrancaMensalEntity c " +
           "WHERE c.status = 'PAGO' " +
           "AND c.recebedor IS NOT NULL " +
           "AND (c.anoReferencia > :anoInicio OR (c.anoReferencia = :anoInicio AND c.mesReferencia >= :mesInicio)) " +
           "AND (c.anoReferencia < :anoFim OR (c.anoReferencia = :anoFim AND c.mesReferencia <= :mesFim))")
    java.math.BigDecimal somarValorProfissionalPagoPorPeriodo(
            @Param("anoInicio") Integer anoInicio,
            @Param("mesInicio") Integer mesInicio,
            @Param("anoFim") Integer anoFim,
            @Param("mesFim") Integer mesFim
    );

    // N+1 fix (P2.4): assinaturas que já têm cobrança gerada num mês/ano, numa query só —
    // GeradorCobrancaService faz o filtro em memória em vez de 1 SELECT de existência por assinatura.
    @Query("SELECT c.assinatura.id FROM CobrancaMensalEntity c " +
           "WHERE c.mesReferencia = :mes AND c.anoReferencia = :ano")
    java.util.Set<Integer> buscarAssinaturaIdsComCobranca(
            @Param("mes") Integer mes,
            @Param("ano") Integer ano
    );

    // Buscar cobranças de uma assinatura
    List<CobrancaMensalEntity> findByAssinatura_Id(Integer assinaturaId);

    // Buscar cobranças pagas no mês para cálculo de faturamento
    @Query("SELECT c FROM CobrancaMensalEntity c " +
           "WHERE c.status = 'PAGO' " +
           "AND c.anoReferencia = :ano " +
           "AND c.mesReferencia = :mes")
    List<CobrancaMensalEntity> findPagasPorMes(
            @Param("ano") Integer ano,
            @Param("mes") Integer mes
    );

    // Listar cobranças com filtro opcional por mês/ano, paginado (P1.3).
    // JOIN FETCH evita N+1 ao montar a descrição; são associações @ManyToOne (to-one),
    // então não multiplicam linhas e a paginação em banco funciona normalmente
    // (diferente de JOIN FETCH em coleções, que o Spring Data pagina em memória).
    // Ordenação vem do Pageable (adapter passa Sort explícito) em vez de fixa na query.
    @Query(value = "SELECT c FROM CobrancaMensalEntity c " +
           "JOIN FETCH c.assinatura a " +
           "JOIN FETCH a.paciente " +
           "JOIN FETCH a.servico " +
           "WHERE (:mes IS NULL OR c.mesReferencia = :mes) " +
           "AND (:ano IS NULL OR c.anoReferencia = :ano)",
           countQuery = "SELECT count(c) FROM CobrancaMensalEntity c " +
           "WHERE (:mes IS NULL OR c.mesReferencia = :mes) " +
           "AND (:ano IS NULL OR c.anoReferencia = :ano)")
    Page<CobrancaMensalEntity> findByMesAnoOpcional(
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            Pageable pageable
    );

    // Buscar cobranças pendentes vencidas (mês atual ou anteriores) para alertas
    // JOIN FETCH evita LazyInitializationException ao ler assinatura.paciente com open-in-view desligado
    @Query("SELECT c FROM CobrancaMensalEntity c " +
           "JOIN FETCH c.assinatura a " +
           "JOIN FETCH a.paciente " +
           "WHERE c.status = 'PENDENTE' " +
           "AND ((c.anoReferencia < :anoAtual) OR " +
           "(c.anoReferencia = :anoAtual AND c.mesReferencia <= :mesAtual)) " +
           "ORDER BY c.anoReferencia ASC, c.mesReferencia ASC")
    List<CobrancaMensalEntity> findPendentesVencidas(
            @Param("anoAtual") Integer anoAtual,
            @Param("mesAtual") Integer mesAtual
    );
}

