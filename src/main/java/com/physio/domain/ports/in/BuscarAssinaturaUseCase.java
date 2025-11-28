package com.physio.domain.ports.in;

import com.physio.domain.model.Assinatura;

import java.util.List;
import java.util.Optional;

public interface BuscarAssinaturaUseCase {
    Optional<Assinatura> buscarPorId(Long id);
    List<Assinatura> listarTodas();
    List<Assinatura> listarPorPaciente(Long pacienteId);
    Optional<Assinatura> buscarAtivaPorPacienteEServico(Long pacienteId, Long servicoId);
}

