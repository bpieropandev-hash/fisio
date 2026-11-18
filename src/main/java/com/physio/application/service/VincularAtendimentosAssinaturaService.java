package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.model.Atendimento;
import com.physio.domain.model.ModalidadeCobranca;
import com.physio.domain.model.StatusAtendimento;
import com.physio.domain.model.StatusPagamento;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ao criar uma assinatura, o que o paciente já tinha agendado como avulso no mesmo serviço, a partir do
 * início da vigência, passa a ser coberto pela mensalidade (mesmo tratamento de um agendamento novo).
 * Antes do início, o histórico não muda. O que já foi pago, cancelado ou faltado também não muda.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VincularAtendimentosAssinaturaService {
    private static final LocalDateTime SEM_FIM = LocalDateTime.of(9999, 12, 31, 23, 59);

    private final AtendimentoRepositoryPort atendimentos;

    /** @return quantos atendimentos passaram a ser cobertos pela assinatura */
    @Transactional
    public int vincular(Assinatura assinatura) {
        if (assinatura.getId() == null || assinatura.getDataInicio() == null) return 0;
        atendimentos.bloquearAgenda();
        LocalDateTime inicio = assinatura.getDataInicio().atStartOfDay();
        LocalDateTime fim = assinatura.getDataCancelamento() != null ? assinatura.getDataCancelamento() : SEM_FIM;
        int vinculados = 0;
        for (Atendimento a : atendimentos.candidatosRemarcacao(assinatura.getPaciente().getId().longValue(),
                assinatura.getServico().getId().longValue(), inicio, fim)) {
            if (!elegivel(a, inicio)) continue;
            a.setAssinaturaId(assinatura.getId());
            a.setModalidadeCobranca(ModalidadeCobranca.MENSALIDADE);
            a.setValorCobrado(BigDecimal.ZERO);
            a.setPctClinicaSnapshot(BigDecimal.ZERO);
            a.setPctProfissionalSnapshot(BigDecimal.ZERO);
            atendimentos.salvar(a);
            vinculados++;
        }
        log.info("Assinatura {}: {} atendimento(s) já agendado(s) passaram a ser mensalidade", assinatura.getId(), vinculados);
        return vinculados;
    }

    private boolean elegivel(Atendimento a, LocalDateTime inicioVigencia) {
        return a.getAssinaturaId() == null
                && a.getModalidadeCobranca() == ModalidadeCobranca.AVULSO
                && (a.getStatus() == StatusAtendimento.AGENDADO || a.getStatus() == StatusAtendimento.CONCLUIDO)
                && a.getStatusPagamento() != StatusPagamento.PAID
                && !a.getDataHoraInicio().isBefore(inicioVigencia);
    }
}
