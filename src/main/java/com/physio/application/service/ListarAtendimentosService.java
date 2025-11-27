package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.in.ListarAtendimentosUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarAtendimentosService implements ListarAtendimentosUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Override
    public List<Atendimento> listarTodos() {
        return atendimentoRepositoryPort.listarTodos();
    }
}

