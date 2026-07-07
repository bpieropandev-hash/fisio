package com.physio.domain.ports.in;

import com.physio.domain.model.Assinatura;

public interface ReativarAssinaturaUseCase {
    Assinatura reativar(Long id);
}
