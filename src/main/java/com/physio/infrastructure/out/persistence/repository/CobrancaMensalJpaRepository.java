package com.physio.infrastructure.out.persistence.repository;

import com.physio.domain.model.StatusCobranca;
import com.physio.infrastructure.out.persistence.entity.CobrancaMensalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaMensalJpaRepository extends JpaRepository<CobrancaMensalEntity, Integer> {

    // Verificar se já existe cobrança para uma assinatura em um mês/ano específico
    Optional<CobrancaMensalEntity> findByAssinatura_IdAndMesReferenciaAndAnoReferencia(
            Integer assinaturaId,
            Integer mesReferencia,
            Integer anoReferencia
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

    // Buscar cobranças pendentes vencidas (mês atual ou anteriores) para alertas
    @Query("SELECT c FROM CobrancaMensalEntity c " +
           "WHERE c.status = 'PENDENTE' " +
           "AND ((c.anoReferencia < :anoAtual) OR " +
           "(c.anoReferencia = :anoAtual AND c.mesReferencia <= :mesAtual)) " +
           "ORDER BY c.anoReferencia ASC, c.mesReferencia ASC")
    List<CobrancaMensalEntity> findPendentesVencidas(
            @Param("anoAtual") Integer anoAtual,
            @Param("mesAtual") Integer mesAtual
    );
}

