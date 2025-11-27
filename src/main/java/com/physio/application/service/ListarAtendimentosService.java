package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.in.ListarAtendimentosUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarAtendimentosService implements ListarAtendimentosUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Override
    public List<Atendimento> listar(LocalDateTime inicio, LocalDateTime fim, Long pacienteId) {
        if (pacienteId != null) {
            return atendimentoRepositoryPort.listarPorPaciente(pacienteId);
        }
        if (inicio != null && fim != null) {
            return atendimentoRepositoryPort.listarPorPeriodo(inicio, fim);
        }
        return atendimentoRepositoryPort.listarTodos();
    }
}