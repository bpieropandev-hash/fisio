package com.physio.application.service;

import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.CriarServicoUseCase;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CriarServicoService implements CriarServicoUseCase {

    private final ServicoRepositoryPort servicoRepositoryPort;
    private final ConfiguracoesService configuracoes;

    @Override
    public ServicoConfig criarServico(ServicoConfig servico) {
        log.info("Criando serviço - Nome: {}", servico.getNome());

        if (servico.getNome() == null || servico.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do serviço é obrigatório");
        }

        // Verificar nome único
        var existente = servicoRepositoryPort.buscarPorNome(servico.getNome());
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Serviço com este nome já existe: " + servico.getNome());
        }

        if (servico.getAtivo() == null) {
            servico.setAtivo(Boolean.TRUE);
        }

        if(servico.getCapacidade()==null) {
            var config=configuracoes.buscar();
            servico.setCapacidade(servico.getTipo()==com.physio.domain.model.TipoServico.PILATES
                    ? config.capacidadePilates() : config.capacidadeFisioterapia());
        }
        if(servico.getCapacidade()<1 || servico.getCapacidade()>100) throw new IllegalArgumentException("Capacidade deve estar entre 1 e 100");
        // Salvar
        ServicoConfig salvo = servicoRepositoryPort.salvar(servico);
        log.info("Serviço criado com ID: {}", salvo.getId());
        return salvo;
    }
}

