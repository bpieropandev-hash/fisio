package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.ReajusteResultado;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.ports.in.ReajustarServicoUseCase;
import com.physio.domain.ports.in.AtualizarServicoUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReajustarServicoService implements ReajustarServicoUseCase {

    private final ServicoRepositoryPort servicoRepositoryPort;
    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;
    private final AtualizarServicoUseCase atualizarServicoUseCase;

    @Override
    @Transactional
    public ReajusteResultado reajustar(Long servicoId, BigDecimal novoValorBase, boolean aplicarEmAssinaturasAtivas,
                                        boolean somenteAssinaturasNoValorAntigo, boolean atualizarCobrancasPendentes) {
        return reajustar(servicoId, novoValorBase, aplicarEmAssinaturasAtivas,
                somenteAssinaturasNoValorAntigo, atualizarCobrancasPendentes, null);
    }

    @Override
    @Transactional
    public ReajusteResultado reajustar(Long servicoId, BigDecimal novoValorBase, boolean aplicarEmAssinaturasAtivas,
            boolean somenteAssinaturasNoValorAntigo, boolean atualizarCobrancasPendentes, ServicoConfig dadosServico) {
        log.info("Reajustando serviço - ID: {}, novoValorBase: {}, aplicarEmAssinaturasAtivas: {}, somenteAssinaturasNoValorAntigo: {}, atualizarCobrancasPendentes: {}",
                servicoId, novoValorBase, aplicarEmAssinaturasAtivas, somenteAssinaturasNoValorAntigo, atualizarCobrancasPendentes);

        ServicoConfig servico = servicoRepositoryPort.buscarPorId(servicoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Serviço não encontrado: " + servicoId));

        BigDecimal valorAntigo = servico.getValorBase();

        // Capture the previous price before editing the catalog; the whole operation shares this transaction.
        if (dadosServico != null) {
            servico = atualizarServicoUseCase.atualizar(servicoId, dadosServico);
        }

        servico.setValorBase(novoValorBase);
        servicoRepositoryPort.salvar(servico);

        int assinaturasAtualizadas = 0;
        List<Integer> assinaturasIgnoradasIds = new ArrayList<>();
        List<Assinatura> atualizadas = new ArrayList<>();

        if (aplicarEmAssinaturasAtivas) {
            List<Assinatura> ativas = assinaturaRepositoryPort.listarAtivasPorServico(servicoId);

            for (Assinatura assinatura : ativas) {
                boolean valorNegociado = valorAntigo == null || assinatura.getValorMensal() == null
                        || valorAntigo.compareTo(assinatura.getValorMensal()) != 0;

                if (somenteAssinaturasNoValorAntigo && valorNegociado) {
                    assinaturasIgnoradasIds.add(assinatura.getId());
                    continue;
                }

                assinatura.setValorMensal(novoValorBase);
                assinaturaRepositoryPort.salvar(assinatura);
                atualizadas.add(assinatura);
                assinaturasAtualizadas++;
            }
        }

        int cobrancasAtualizadas = 0;

        if (atualizarCobrancasPendentes) {
            LocalDate hoje = LocalDate.now();
            Integer mesAtual = hoje.getMonthValue();
            Integer anoAtual = hoje.getYear();

            for (Assinatura assinatura : atualizadas) {
                CobrancaMensal cobranca = cobrancaMensalRepositoryPort
                        .buscarPorAssinaturaMesAno(assinatura.getId().longValue(), mesAtual, anoAtual)
                        .orElse(null);

                if (cobranca != null && cobranca.getStatus() == StatusCobranca.PENDENTE) {
                    cobranca.setValor(novoValorBase);
                    cobrancaMensalRepositoryPort.salvar(cobranca);
                    cobrancasAtualizadas++;
                }
            }
        }

        log.info("Reajuste concluído - Serviço: {}, assinaturasAtualizadas: {}, cobrancasAtualizadas: {}, ignoradas: {}",
                servicoId, assinaturasAtualizadas, cobrancasAtualizadas, assinaturasIgnoradasIds);

        return ReajusteResultado.builder()
                .assinaturasAtualizadas(assinaturasAtualizadas)
                .cobrancasAtualizadas(cobrancasAtualizadas)
                .assinaturasIgnoradasIds(assinaturasIgnoradasIds)
                .build();
    }
}
