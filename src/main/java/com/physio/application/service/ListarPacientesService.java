package com.physio.application.service;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.in.ListarPacientesUseCase;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarPacientesService implements ListarPacientesUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;

    @Override
    public List<Paciente> listarTodos() {
        return pacienteRepositoryPort.listarTodos();
    }
}

