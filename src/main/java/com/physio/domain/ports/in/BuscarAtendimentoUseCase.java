package com.physio.domain.ports.in;

import com.physio.domain.model.Atendimento;

import java.util.Optional;

public interface BuscarAtendimentoUseCase {
    Optional<Atendimento> buscarPorId(Long id);
}

