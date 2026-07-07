package com.physio.application.service;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.in.ReativarPacienteUseCase;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReativarPacienteService implements ReativarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;

    @Override
    @Transactional
    public Paciente reativar(Long id) {
        log.info("Reativando paciente - ID: {}", id);

        Paciente paciente = pacienteRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Paciente não encontrado: " + id));

        paciente.setAtivo(true);
        Paciente salvo = pacienteRepositoryPort.salvar(paciente);

        log.info("Paciente reativado com sucesso - ID: {}", id);
        return salvo;
    }
}
