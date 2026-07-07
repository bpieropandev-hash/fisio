package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoReajusteResponseDTO {
    @Schema(example = "28")
    private Integer assinaturasAtualizadas;

    @Schema(example = "0")
    private Integer cobrancasAtualizadas;

    @Schema(description = "IDs de assinaturas com valor negociado, ignoradas no reajuste", example = "[35]")
    private List<Integer> assinaturasIgnoradasIds;
}
