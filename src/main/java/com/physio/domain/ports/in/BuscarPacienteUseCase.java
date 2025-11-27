package com.physio.domain.ports.in;

import com.physio.domain.model.Paciente;

import java.util.Optional;

public interface BuscarPacienteUseCase {
    Optional<Paciente> buscarPorId(Long id);
}

