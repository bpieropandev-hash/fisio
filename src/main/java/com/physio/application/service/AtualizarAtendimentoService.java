package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.in.AtualizarAtendimentoUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtualizarAtendimentoService implements AtualizarAtendimentoUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Override
    public Atendimento atualizar(Long id, Atendimento atendimento) {
        log.info("Atualizando atendimento - ID: {}", id);
        var existente = atendimentoRepositoryPort.buscarPorId(id);
        if (existente == null) throw new IllegalArgumentException("Atendimento não encontrado: " + id);

        if (atendimento.getDataHoraInicio() != null) existente.setDataHoraInicio(atendimento.getDataHoraInicio());
        if (atendimento.getDataHoraFim() != null) existente.setDataHoraFim(atendimento.getDataHoraFim());
        if (atendimento.getStatus() != null) existente.setStatus(atendimento.getStatus());
        if (atendimento.getEvolucao() != null) existente.setEvolucao(atendimento.getEvolucao());
        if(atendimento.getRecebedor() != null) existente.setRecebedor(atendimento.getRecebedor());
        if (atendimento.getTipoPagamento() != null) existente.setTipoPagamento(atendimento.getTipoPagamento());

        var salvo = atendimentoRepositoryPort.salvar(existente);
        log.info("Atendimento atualizado - ID: {}", salvo.getId());
        return salvo;
    }
}

