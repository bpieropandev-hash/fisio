package com.physio.domain.ports.out;

import com.physio.domain.model.Paciente;

import java.util.List;
import java.util.Optional;

public interface PacienteRepositoryPort {
    Optional<Paciente> buscarPorId(Long id);
    Optional<Paciente> buscarPorCpf(String cpf);
    Paciente salvar(Paciente paciente);
    List<Paciente> listarTodos();
}
