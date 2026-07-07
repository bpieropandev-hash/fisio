package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaTrocaPlanoResponseDTO {
    private AssinaturaResponseDTO novaAssinatura;

    @Schema(description = "Se true, a assinatura antiga já tinha cobrança gerada no mês corrente e ela foi mantida intocada")
    private boolean cobrancaMesAtualMantida;
}
