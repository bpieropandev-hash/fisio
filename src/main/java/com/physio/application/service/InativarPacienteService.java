package com.physio.application.service;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.in.InativarPacienteUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InativarPacienteService implements InativarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Override
    @Transactional
    public void inativar(Long id) {
        log.info("Inativando paciente - ID: {}", id);

        Paciente paciente = pacienteRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Paciente não encontrado: " + id));

        boolean temAssinaturaAtiva = assinaturaRepositoryPort.listarPorPaciente(id).stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getAtivo()));

        if (temAssinaturaAtiva) {
            throw new IllegalArgumentException(
                    "Paciente possui assinatura ativa. Cancele ou reative para outro paciente antes de inativar: " + id);
        }

        if (paciente.getAtivo() == null || !paciente.getAtivo()) {
            log.info("Paciente já está inativo - ID: {}", id);
            return;
        }

        paciente.setAtivo(false);
        pacienteRepositoryPort.salvar(paciente);

        log.info("Paciente inativado com sucesso - ID: {}", id);
    }
}
