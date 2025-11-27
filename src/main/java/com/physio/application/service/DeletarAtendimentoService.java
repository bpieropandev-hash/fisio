package com.physio.application.service;

import com.physio.domain.ports.in.DeletarAtendimentoUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeletarAtendimentoService implements DeletarAtendimentoUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Override
    public void deletar(Long id) {
        log.info("Deletando atendimento - ID: {}", id);
        atendimentoRepositoryPort.deletar(id);
    }
}

