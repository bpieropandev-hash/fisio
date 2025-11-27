package com.physio.domain.ports.in;

import com.physio.domain.model.Paciente;

import java.util.List;

public interface ListarPacientesUseCase {
    List<Paciente> listarTodos();
}

