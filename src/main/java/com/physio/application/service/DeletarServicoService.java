package com.physio.application.service;

import com.physio.domain.ports.in.DeletarServicoUseCase;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeletarServicoService implements DeletarServicoUseCase {

    private final ServicoRepositoryPort servicoRepositoryPort;

    @Override
    public void deletar(Long id) {
        log.info("Desativando serviço (soft-delete) - ID: {}", id);
        servicoRepositoryPort.desativar(id);
    }
}
