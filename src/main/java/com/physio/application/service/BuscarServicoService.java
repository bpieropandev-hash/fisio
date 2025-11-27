package com.physio.application.service;

import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.BuscarServicoUseCase;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuscarServicoService implements BuscarServicoUseCase {

    private final ServicoRepositoryPort servicoRepositoryPort;

    @Override
    public Optional<ServicoConfig> buscarPorId(Long id) {
        return servicoRepositoryPort.buscarPorId(id);
    }
}

