package com.physio.domain.ports.in;

import com.physio.domain.model.ServicoConfig;

public interface CriarServicoUseCase {
    ServicoConfig criarServico(ServicoConfig servico);
}

