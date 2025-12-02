package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.in.CancelarAssinaturaUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelarAssinaturaService implements CancelarAssinaturaUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Override
    @Transactional
    public void cancelar(Long id) {
        log.info("Cancelando assinatura - ID: {}", id);

        Assinatura assinatura = assinaturaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Assinatura não encontrada: " + id));

        if (assinatura.getAtivo() == null || !assinatura.getAtivo()) {
            log.info("Assinatura já está inativa - ID: {}", id);
            return; // idempotente
        }

        assinatura.setAtivo(false);
        assinatura.setDataCancelamento(LocalDateTime.now());
        assinaturaRepositoryPort.salvar(assinatura);

        log.info("Assinatura cancelada com sucesso - ID: {}", id);
    }
}
