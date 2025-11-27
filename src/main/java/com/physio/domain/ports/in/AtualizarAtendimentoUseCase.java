package com.physio.domain.ports.in;

import com.physio.domain.model.Atendimento;

public interface AtualizarAtendimentoUseCase {
    Atendimento atualizar(Long id, Atendimento atendimento);
}

