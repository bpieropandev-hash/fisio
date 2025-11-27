package com.physio.domain.ports.in;

import com.physio.domain.model.Paciente;

public interface AtualizarPacienteUseCase {
    Paciente atualizar(Long id, Paciente paciente);
}

