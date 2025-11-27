package com.physio.domain.ports.out;

import com.physio.domain.model.Atendimento;

import java.util.List;

public interface AtendimentoRepositoryPort {
    Atendimento salvar(Atendimento atendimento);
    Atendimento buscarPorId(Long id);
    List<Atendimento> listarTodos();
    void deletar(Long id);
}
