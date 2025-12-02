package com.physio.application.service;

import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.Recebedor;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.model.TipoPagamento;
import com.physio.domain.ports.in.AtualizarCobrancaUseCase;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtualizarCobrancaService implements AtualizarCobrancaUseCase {

    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Override
    @Transactional
    public CobrancaMensal atualizarCobranca(Long cobrancaId, StatusCobranca status, LocalDate dataPagamento, Recebedor recebedor, TipoPagamento tipoPagamento) {
        log.info("Atualizando cobrança - ID: {}, Status: {}", cobrancaId, status);

        CobrancaMensal cobranca = cobrancaMensalRepositoryPort.buscarPorId(cobrancaId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cobrança não encontrada: " + cobrancaId));

        // Atualizar campos
        cobranca.setStatus(status);
        cobranca.setDataPagamento(dataPagamento);
        cobranca.setRecebedor(recebedor);
        cobranca.setTipoPagamento(tipoPagamento);

        // Validação: Se status é PAGO, deve ter data de pagamento e recebedor
        if (status == StatusCobranca.PAGO) {
            if (dataPagamento == null) {
                throw new IllegalArgumentException("Data de pagamento é obrigatória quando status é PAGO");
            }
            if (recebedor == null) {
                throw new IllegalArgumentException("Recebedor é obrigatório quando status é PAGO");
            }
        }

        // Salvar atualização
        CobrancaMensal cobrancaAtualizada = cobrancaMensalRepositoryPort.salvar(cobranca);

        log.info("Cobrança atualizada com sucesso - ID: {}", cobrancaAtualizada.getId());
        return cobrancaAtualizada;
    }
}
