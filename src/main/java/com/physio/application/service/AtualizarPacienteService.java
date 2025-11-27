package com.physio.application.service;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.in.AtualizarPacienteUseCase;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtualizarPacienteService implements AtualizarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;

    @Override
    public Paciente atualizar(Long id, Paciente paciente) {
        log.info("Atualizando paciente - ID: {}", id);
        var existenteOpt = pacienteRepositoryPort.buscarPorId(id);
        var existente = existenteOpt.orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado: " + id));

        if (paciente.getNome() != null) existente.setNome(paciente.getNome());
        if (paciente.getCpf() != null) existente.setCpf(paciente.getCpf());
        if (paciente.getDataNascimento() != null) existente.setDataNascimento(paciente.getDataNascimento());
        if (paciente.getTelefone() != null) existente.setTelefone(paciente.getTelefone());
        if (paciente.getEmail() != null) existente.setEmail(paciente.getEmail());
        if (paciente.getLogradouro() != null) existente.setLogradouro(paciente.getLogradouro());
        if (paciente.getNumero() != null) existente.setNumero(paciente.getNumero());
        if (paciente.getBairro() != null) existente.setBairro(paciente.getBairro());
        if (paciente.getCidade() != null) existente.setCidade(paciente.getCidade());
        if (paciente.getEstado() != null) existente.setEstado(paciente.getEstado());
        if (paciente.getCep() != null) existente.setCep(paciente.getCep());
        if (paciente.getComplemento() != null) existente.setComplemento(paciente.getComplemento());
        if (paciente.getAnamnese() != null) existente.setAnamnese(paciente.getAnamnese());

        var salvo = pacienteRepositoryPort.salvar(existente);
        log.info("Paciente atualizado - ID: {}", salvo.getId());
        return salvo;
    }
}
