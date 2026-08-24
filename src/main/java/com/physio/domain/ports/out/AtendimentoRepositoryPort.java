package com.physio.domain.ports.out;

import com.physio.domain.model.Atendimento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AtendimentoRepositoryPort {
    Atendimento salvar(Atendimento atendimento);
    Atendimento buscarPorId(Long id);
    List<Atendimento> listarTodos();
    void deletar(Long id);

    List<Atendimento> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);
    List<Atendimento> listarPorPaciente(Long pacienteId);
    List<Atendimento> listarConflitosPorPeriodo(LocalDateTime inicio, LocalDateTime fim);

    // Usado pelo relatório de acerto financeiro (recebedor definido, filtro opcional por serviço)
    List<Atendimento> listarParaRelatorioFinanceiro(LocalDateTime inicio, LocalDateTime fim, List<Integer> servicoIds);

    // Usados pelo dashboard (P2.2/P2.3 — agregação em banco em vez de somar lista em Java)
    Long contarConcluidosPorMes(LocalDateTime inicio, LocalDateTime fim);
    Long contarConcluidosComPagamentoPendentePorMes(LocalDateTime inicio, LocalDateTime fim);
    BigDecimal somarFaturamentoProfissionalAvulsos(LocalDateTime inicio, LocalDateTime fim);
    List<Atendimento> listarConcluidosComPagamentoPendente();
}
