package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.in.BuscarAtendimentoUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuscarAtendimentoService implements BuscarAtendimentoUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Override
    public Optional<Atendimento> buscarPorId(Long id) {
        return java.util.Optional.ofNullable(atendimentoRepositoryPort.buscarPorId(id));
    }
}

