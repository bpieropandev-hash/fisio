package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.model.StatusPagamento;
import com.physio.domain.ports.in.AtualizarAtendimentoUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtualizarAtendimentoService implements AtualizarAtendimentoUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Override
    public Atendimento atualizar(Long id, Atendimento atendimento) {
        log.info("Atualizando atendimento - ID: {}", id);
        var existente = atendimentoRepositoryPort.buscarPorId(id);
        if (existente == null) throw new IllegalArgumentException("Atendimento não encontrado: " + id);

        if (atendimento.getDataHoraInicio() != null) {
            // Se só o início mudou, desloca o fim junto para preservar a duração —
            // senão o atendimento fica com fim anterior ao início
            if (atendimento.getDataHoraFim() == null
                    && existente.getDataHoraInicio() != null
                    && existente.getDataHoraFim() != null) {
                var duracao = java.time.Duration.between(existente.getDataHoraInicio(), existente.getDataHoraFim());
                existente.setDataHoraFim(atendimento.getDataHoraInicio().plus(duracao));
            }
            existente.setDataHoraInicio(atendimento.getDataHoraInicio());
        }
        if (atendimento.getDataHoraFim() != null) existente.setDataHoraFim(atendimento.getDataHoraFim());
        if (atendimento.getStatus() != null) existente.setStatus(atendimento.getStatus());
        if (atendimento.getEvolucao() != null) existente.setEvolucao(atendimento.getEvolucao());
        
        // REGRA DE NEGÓCIO: Verificar se é atendimento com assinatura (valorCobrado = 0)
        boolean temAssinatura = existente.getValorCobrado() != null && 
                                 existente.getValorCobrado().compareTo(java.math.BigDecimal.ZERO) == 0;
        
        if (temAssinatura) {
            // Para atendimentos com assinatura, não devemos permitir recebedor/tipoPagamento
            // O pagamento é feito via mensalidade (CobrancaMensal) no fim do mês
            // Limpar campos de pagamento se tentarem ser atualizados
            if (atendimento.getRecebedor() != null || atendimento.getTipoPagamento() != null) {
                log.warn("Tentativa de atualizar recebedor/tipoPagamento em atendimento com assinatura (ID: {}). " +
                         "Campos ignorados - pagamento é via mensalidade.", id);
            }
            existente.setRecebedor(null);
            existente.setTipoPagamento(null);
            existente.setStatusPagamento(StatusPagamento.PAYMENT_PENDING);
            log.debug("Atendimento {} tem assinatura - pagamento será via mensalidade, não via atendimento", id);
        } else {
            // Para atendimentos avulsos (valorCobrado > 0), aplicar lógica normal de pagamento
            // Atualizar campos de pagamento (podem ser null - não são obrigatórios)
            if (atendimento.getRecebedor() != null) {
                existente.setRecebedor(atendimento.getRecebedor());
            }
            
            if (atendimento.getTipoPagamento() != null) {
                existente.setTipoPagamento(atendimento.getTipoPagamento());
            }
            
            // Determinar status de pagamento baseado na presença dos dados de pagamento
            // Regra de negócio: ambos (recebedor E tipoPagamento) devem estar presentes para ser PAID
            // Caso contrário, marca como PAYMENT_PENDING
            if (existente.getRecebedor() != null && existente.getTipoPagamento() != null) {
                existente.setStatusPagamento(StatusPagamento.PAID);
                log.debug("Atendimento {} marcado como PAID - recebedor e tipoPagamento presentes", id);
            } else {
                existente.setStatusPagamento(StatusPagamento.PAYMENT_PENDING);
                log.debug("Atendimento {} marcado como PAYMENT_PENDING - dados de pagamento ausentes ou incompletos", id);
            }
        }

        var salvo = atendimentoRepositoryPort.salvar(existente);
        log.info("Atendimento atualizado - ID: {}, Status Pagamento: {}", salvo.getId(), salvo.getStatusPagamento());
        return salvo;
    }
}

