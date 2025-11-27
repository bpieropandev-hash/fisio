package com.physio.domain.ports.out;

import com.physio.domain.model.ServicoConfig;

import java.util.List;
import java.util.Optional;

public interface ServicoRepositoryPort {
    Optional<ServicoConfig> buscarPorId(Long id);
    Optional<ServicoConfig> buscarPorIdEAtivo(Long id);
    Optional<ServicoConfig> buscarPorNome(String nome);
    ServicoConfig salvar(ServicoConfig servico);
    List<ServicoConfig> listarTodos();
    void desativar(Long id);
}
