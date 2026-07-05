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

@Slf4j
@Service
@RequiredArgsConstructor
public class GeradorCobrancaService implements GerarCobrancasUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    /**
     * Gera as cobranças do mês para todas as assinaturas ativas. Idempotente em duas
     * camadas: check por (assinatura, mês, ano) + constraint UNIQUE no banco (V5).
     * Cada cobrança é salva na própria transação: a falha de uma não impede as demais
     * (por isso NÃO há @Transactional no método).
     */
    @Override
    public int gerarCobrancasDoMes(int mes, int ano) {
        log.info("Iniciando geração de cobranças para mês: {}/{}", mes, ano);

        List<Assinatura> assinaturasAtivas = assinaturaRepositoryPort.listarAtivas();
        log.info("Encontradas {} assinaturas ativas", assinaturasAtivas.size());

        int cobrancasGeradas = 0;

        for (Assinatura assinatura : assinaturasAtivas) {
            boolean jaExiste = cobrancaMensalRepositoryPort
                    .buscarPorAssinaturaMesAno(Long.valueOf(assinatura.getId()), mes, ano)
                    .isPresent();

            if (jaExiste) {
                log.debug("Cobrança já existe para assinatura ID: {} - mês: {}/{}",
                        assinatura.getId(), mes, ano);
                continue;
            }

            CobrancaMensal novaCobranca = CobrancaMensal.builder()
                    .assinatura(assinatura)
                    .mesReferencia(mes)
                    .anoReferencia(ano)
                    .valor(assinatura.getValorMensal())
                    .status(StatusCobranca.PENDENTE)
                    .build();

            novaCobranca.criarSnapshotFinanceiro(assinatura.getServico());

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
