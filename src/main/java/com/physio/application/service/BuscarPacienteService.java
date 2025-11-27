package com.physio.application.service;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.in.BuscarPacienteUseCase;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuscarPacienteService implements BuscarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;

    @Override
    public Optional<Paciente> buscarPorId(Long id) {
        return pacienteRepositoryPort.buscarPorId(id);
    }
}

