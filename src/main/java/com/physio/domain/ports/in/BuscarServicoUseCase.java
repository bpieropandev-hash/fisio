package com.physio.domain.ports.in;

import com.physio.domain.model.ServicoConfig;

import java.util.Optional;

public interface BuscarServicoUseCase {
    Optional<ServicoConfig> buscarPorId(Long id);
}

