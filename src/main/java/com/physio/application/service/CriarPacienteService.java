package com.physio.application.service;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.in.CriarPacienteUseCase;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CriarPacienteService implements CriarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;

    @Override
    public Paciente criarPaciente(Paciente paciente) {
        log.info("Criando paciente - CPF: {}", paciente.getCpf());

        // Validar CPF não nulo
        if (paciente.getCpf() == null || paciente.getCpf().isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }

        // Verificar se já existe paciente com mesmo CPF
        var existente = pacienteRepositoryPort.buscarPorCpf(paciente.getCpf());
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Paciente com este CPF já existe: " + paciente.getCpf());
        }

        // Preencher campos default
        if (paciente.getDataCadastro() == null) {
            paciente.setDataCadastro(LocalDateTime.now());
        }
        if (paciente.getAnamnese() == null) {
            paciente.setAnamnese("");
        }

        paciente.setAtivo(true);


        // Salvar via porta
        Paciente salvo = pacienteRepositoryPort.salvar(paciente);
        log.info("Paciente criado com ID: {}", salvo.getId());
        return salvo;
    }
}
