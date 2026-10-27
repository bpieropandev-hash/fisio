package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.ports.in.GerarCobrancasUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeradorCobrancaService implements GerarCobrancasUseCase, com.physio.domain.ports.in.GerarCobrancaAssinaturaUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Override
    public boolean gerarCobrancaMesAtual(Long assinaturaId) {
        return gerarParaAssinatura(assinaturaId,
                java.time.YearMonth.now(java.time.ZoneId.of("America/Sao_Paulo")));
    }

    // Sem transação externa: uma colisão UNIQUE encerra apenas a transação do save,
    // permitindo consultar a cobrança criada pela execução concorrente.
    boolean gerarParaAssinatura(Long assinaturaId, java.time.YearMonth competencia) {
        Assinatura assinatura = assinaturaRepositoryPort.buscarPorId(assinaturaId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Assinatura não encontrada: " + assinaturaId));
        if (!Boolean.TRUE.equals(assinatura.getAtivo())) {
            throw new IllegalArgumentException("Reative a assinatura antes de gerar a cobrança.");
        }
        if (!estaVigente(assinatura, competencia)) {
            throw new IllegalArgumentException("Assinatura fora de vigência no mês corrente.");
        }
        if (assinatura.getPaciente() != null && Boolean.FALSE.equals(assinatura.getPaciente().getAtivo())) {
            throw new IllegalArgumentException("Paciente não está ativo.");
        }
        if (Boolean.FALSE.equals(assinatura.getServico().getAtivo())) {
            throw new IllegalArgumentException("Serviço não está ativo.");
        }
        int mes = competencia.getMonthValue();
        int ano = competencia.getYear();
        if (cobrancaMensalRepositoryPort.buscarPorAssinaturaMesAno(assinaturaId, mes, ano).isPresent()) return false;
        try {
            cobrancaMensalRepositoryPort.salvar(novaCobranca(assinatura, mes, ano));
            return true;
        } catch (DataIntegrityViolationException e) {
            if (cobrancaMensalRepositoryPort.buscarPorAssinaturaMesAno(assinaturaId, mes, ano).isPresent()) return false;
            throw e;
        }
    }

    private boolean estaVigente(Assinatura assinatura, java.time.YearMonth competencia) {
        return (assinatura.getDataInicio() == null || !competencia.isBefore(java.time.YearMonth.from(assinatura.getDataInicio())))
                && (assinatura.getDataCancelamento() == null || !competencia.isAfter(java.time.YearMonth.from(assinatura.getDataCancelamento())));
    }

    private CobrancaMensal novaCobranca(Assinatura assinatura, int mes, int ano) {
        CobrancaMensal cobranca = CobrancaMensal.builder()
                .assinatura(assinatura).mesReferencia(mes).anoReferencia(ano)
                .valor(assinatura.getValorMensal()).status(StatusCobranca.PENDENTE).build();
        cobranca.criarSnapshotFinanceiro(assinatura.getServico());
        return cobranca;
    }

    /**
     * Gera as cobranças do mês para todas as assinaturas ativas. Idempotente em duas
     * camadas: check por (assinatura, mês, ano) + constraint UNIQUE no banco (V5).
     * Cada cobrança é salva na própria transação: a falha de uma não impede as demais
     * (por isso NÃO há @Transactional no método).
     */
    @Override
    public int gerarCobrancasDoMes(int mes, int ano) {
        log.info("Iniciando geração de cobranças para mês: {}/{}", mes, ano);

        var competencia = java.time.YearMonth.of(ano, mes);
        List<Assinatura> assinaturasAtivas = assinaturaRepositoryPort.listarAtivas();
        log.info("Encontradas {} assinaturas ativas", assinaturasAtivas.size());

        // P2.4: 1 query pra saber quais assinaturas já têm cobrança no mês, em vez de
        // 1 query de existência por assinatura dentro do loop (N+1).
        Set<Integer> assinaturaIdsComCobranca = cobrancaMensalRepositoryPort
                .buscarAssinaturaIdsComCobranca(mes, ano);

        int cobrancasGeradas = 0;

        for (Assinatura assinatura : assinaturasAtivas) {
            if (!estaVigente(assinatura, competencia)) continue;
            boolean jaExiste = assinaturaIdsComCobranca.contains(assinatura.getId());

            if (jaExiste) {
                log.debug("Cobrança já existe para assinatura ID: {} - mês: {}/{}",
                        assinatura.getId(), mes, ano);
                continue;
            }

            CobrancaMensal novaCobranca = novaCobranca(assinatura, mes, ano);

            try {
                cobrancaMensalRepositoryPort.salvar(novaCobranca);
                cobrancasGeradas++;
                log.info("Cobrança gerada - Assinatura ID: {}, Valor: R$ {}, Mês: {}/{}",
                        assinatura.getId(), novaCobranca.getValor(), mes, ano);
            } catch (DataIntegrityViolationException e) {
                // Corrida com outra execução (scheduler x geração manual): a constraint
                // UNIQUE garantiu a unicidade — apenas registra e segue
                log.warn("Cobrança já criada concorrentemente para assinatura ID: {} - mês: {}/{}",
                        assinatura.getId(), mes, ano);
            }
        }

        log.info("Geração de cobranças concluída. Total gerado: {}", cobrancasGeradas);
        return cobrancasGeradas;
    }
}
