package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.in.ReativarAssinaturaUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReativarAssinaturaService implements ReativarAssinaturaUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Override
    @Transactional
    public Assinatura reativar(Long id) {
        log.info("Reativando assinatura - ID: {}", id);

        Assinatura assinatura = assinaturaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Assinatura não encontrada: " + id));

        if (assinatura.getAtivo() != null && assinatura.getAtivo()) {
            log.info("Assinatura já está ativa - ID: {}", id);
            return assinatura;
        }

        Long pacienteId = assinatura.getPaciente().getId().longValue();
        Long servicoId = assinatura.getServico().getId().longValue();

        boolean jaExisteOutraAtiva = assinaturaRepositoryPort
                .buscarAtivaPorPacienteEServico(pacienteId, servicoId)
                .isPresent();

        if (jaExisteOutraAtiva) {
            throw new IllegalArgumentException(
                    "Já existe uma assinatura ativa para o paciente " + pacienteId + " e serviço " + servicoId);
        }

        assinatura.setAtivo(true);
        assinatura.setDataCancelamento(null);
        Assinatura salva = assinaturaRepositoryPort.salvar(assinatura);

        log.info("Assinatura reativada com sucesso - ID: {}", id);
        return salva;
    }
}
