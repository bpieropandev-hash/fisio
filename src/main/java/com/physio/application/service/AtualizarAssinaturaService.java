package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.in.AtualizarAssinaturaUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtualizarAssinaturaService implements AtualizarAssinaturaUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Override
    @Transactional
    public Assinatura atualizar(Long id, BigDecimal valorMensal, Integer diaVencimento) {
        log.info("Atualizando assinatura - ID: {}", id);

        Assinatura assinatura = assinaturaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Assinatura não encontrada: " + id));

        if (assinatura.getAtivo() == null || !assinatura.getAtivo()) {
            throw new IllegalArgumentException("Não é possível editar uma assinatura cancelada: " + id);
        }

        if (diaVencimento != null && (diaVencimento < 1 || diaVencimento > 28)) {
            throw new IllegalArgumentException("Dia de vencimento deve estar entre 1 e 28");
        }

        if (valorMensal != null) {
            assinatura.setValorMensal(valorMensal);
        }
        if (diaVencimento != null) {
            assinatura.setDiaVencimento(diaVencimento);
        }

        Assinatura salva = assinaturaRepositoryPort.salvar(assinatura);

        log.info("Assinatura atualizada com sucesso - ID: {}", id);
        return salva;
    }
}
