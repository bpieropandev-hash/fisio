package com.physio.domain.ports.in;

import com.physio.domain.model.ServicoConfig;

import java.util.List;

public interface ListarServicosUseCase {
    List<ServicoConfig> listarTodos();
}

