package com.physio.infrastructure.in.web.dto;

import com.physio.domain.model.Recebedor;
import com.physio.domain.model.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ConcluirAtendimentoLoteItemDTO(
        @NotNull(message = "ID do atendimento é obrigatório")
        Long id,

        @Schema(description = "Obrigatória para concluir; se ausente, o item entra em 'falhas' na resposta")
        String evolucao,

        @Schema(description = "Só considerado para atendimentos avulsos (mensalistas pagam via cobrança mensal)")
        Recebedor recebedor,

        TipoPagamento tipoPagamento
) {
}
