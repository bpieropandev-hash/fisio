package com.physio.application.service;

import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.ListarServicosUseCase;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarServicosService implements ListarServicosUseCase {

    private final ServicoRepositoryPort servicoRepositoryPort;

    @Override
    public List<ServicoConfig> listarTodos() {
        return servicoRepositoryPort.listarTodos();
    }
}

