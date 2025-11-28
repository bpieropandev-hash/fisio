package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.in.BuscarAssinaturaUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuscarAssinaturaService implements BuscarAssinaturaUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Override
    public Optional<Assinatura> buscarPorId(Long id) {
        return assinaturaRepositoryPort.buscarPorId(id);
    }

    @Override
    public List<Assinatura> listarTodas() {
        return assinaturaRepositoryPort.listarTodas();
    }

    @Override
    public List<Assinatura> listarPorPaciente(Long pacienteId) {
        return assinaturaRepositoryPort.listarPorPaciente(pacienteId);
    }

    @Override
    public Optional<Assinatura> buscarAtivaPorPacienteEServico(Long pacienteId, Long servicoId) {
        return assinaturaRepositoryPort.buscarAtivaPorPacienteEServico(pacienteId, servicoId);
    }
}

