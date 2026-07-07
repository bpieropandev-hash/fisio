package com.physio.application.service;

import com.physio.domain.ports.in.ContarAssinaturasAtivasPorServicoUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContarAssinaturasAtivasPorServicoService implements ContarAssinaturasAtivasPorServicoUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Override
    public long contar(Long servicoId) {
        return assinaturaRepositoryPort.listarAtivasPorServico(servicoId).size();
    }
}
