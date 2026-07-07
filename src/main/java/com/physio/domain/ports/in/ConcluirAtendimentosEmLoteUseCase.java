package com.physio.domain.ports.in;

import com.physio.domain.model.Recebedor;
import com.physio.domain.model.TipoPagamento;

import java.util.List;

public interface ConcluirAtendimentosEmLoteUseCase {

    ResultadoConclusaoLote concluir(List<ItemConclusaoLote> itens);

    record ItemConclusaoLote(Long id, String evolucao, Recebedor recebedor, TipoPagamento tipoPagamento) {
    }

    record FalhaConclusaoLote(Long atendimentoId, String motivo) {
    }

    record ResultadoConclusaoLote(int concluidos, List<FalhaConclusaoLote> falhas) {
    }
}
