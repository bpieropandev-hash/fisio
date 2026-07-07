package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.in.AtualizarAtendimentoUseCase;
import com.physio.domain.ports.in.ConcluirAtendimentosEmLoteUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcluirAtendimentosEmLoteService implements ConcluirAtendimentosEmLoteUseCase {

    private final AtualizarAtendimentoUseCase atualizarAtendimentoUseCase;

    /**
     * Cada item é processado numa chamada separada de {@link AtualizarAtendimentoUseCase}
     * (transacional por chamada via JpaRepository.save) — falha de um item não derruba os outros.
     */
    @Override
    public ResultadoConclusaoLote concluir(List<ItemConclusaoLote> itens) {
        int concluidos = 0;
        List<FalhaConclusaoLote> falhas = new ArrayList<>();

        for (ItemConclusaoLote item : itens) {
            if (item.evolucao() == null || item.evolucao().isBlank()) {
                falhas.add(new FalhaConclusaoLote(item.id(), "Evolução é obrigatória para concluir o atendimento"));
                continue;
            }

            try {
                Atendimento domain = Atendimento.builder()
                        .status("CONCLUIDO")
                        .evolucao(item.evolucao())
                        .recebedor(item.recebedor())
                        .tipoPagamento(item.tipoPagamento())
                        .build();

                atualizarAtendimentoUseCase.atualizar(item.id(), domain);
                concluidos++;
            } catch (Exception e) {
                log.warn("Falha ao concluir atendimento em lote - ID: {} - Motivo: {}", item.id(), e.getMessage());
                falhas.add(new FalhaConclusaoLote(item.id(), e.getMessage() != null ? e.getMessage() : "Erro ao concluir atendimento"));
            }
        }

        log.info("Conclusão em lote finalizada - {} concluído(s), {} falha(s)", concluidos, falhas.size());
        return new ResultadoConclusaoLote(concluidos, falhas);
    }
}
