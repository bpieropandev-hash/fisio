package com.physio.domain.ports.in;

import com.physio.domain.model.Paciente;

public interface CriarPacienteUseCase {
    Paciente criarPaciente(Paciente paciente);
}

