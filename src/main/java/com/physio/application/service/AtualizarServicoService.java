package com.physio.application.service;

import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.AtualizarServicoUseCase;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtualizarServicoService implements AtualizarServicoUseCase {

    private final ServicoRepositoryPort servicoRepositoryPort;

    @Override
    public ServicoConfig atualizar(Long id, ServicoConfig servico) {
        log.info("Atualizando serviço - ID: {}", id);
        var existenteOpt = servicoRepositoryPort.buscarPorId(id);
        var existente = existenteOpt.orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + id));

        if (servico.getNome() != null && !servico.getNome().equals(existente.getNome())) {
            var porNome = servicoRepositoryPort.buscarPorNome(servico.getNome());
            if (porNome.isPresent()) {
                throw new IllegalArgumentException("Outro serviço com este nome já existe: " + servico.getNome());
            }
            existente.setNome(servico.getNome());
        }

        if (servico.getValorBase() != null) existente.setValorBase(servico.getValorBase());
        if (servico.getPctClinica() != null) existente.setPctClinica(servico.getPctClinica());
        if (servico.getPctProfissional() != null) existente.setPctProfissional(servico.getPctProfissional());
        if (servico.getAtivo() != null) existente.setAtivo(servico.getAtivo());

        var salvo = servicoRepositoryPort.salvar(existente);
        log.info("Serviço atualizado - ID: {}", salvo.getId());
        return salvo;
    }
}

