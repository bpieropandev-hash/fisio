package com.physio.domain.ports.in;

import com.physio.domain.model.Atendimento;

import java.util.List;

public interface ListarAtendimentosUseCase {
    List<Atendimento> listarTodos();
}

