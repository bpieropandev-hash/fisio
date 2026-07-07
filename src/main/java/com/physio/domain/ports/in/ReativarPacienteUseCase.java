package com.physio.domain.ports.in;

import com.physio.domain.model.Paciente;

public interface ReativarPacienteUseCase {
    Paciente reativar(Long id);
}
