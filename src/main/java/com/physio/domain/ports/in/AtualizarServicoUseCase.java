package com.physio.domain.ports.in;

import com.physio.domain.model.ServicoConfig;

public interface AtualizarServicoUseCase {
    ServicoConfig atualizar(Long id, ServicoConfig servico);
}

