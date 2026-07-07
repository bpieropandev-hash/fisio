package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaUpdateRequestDTO {
    @Min(value = 0, message = "Valor mensal deve ser positivo")
    @Schema(description = "Novo valor da mensalidade (opcional)", example = "320.00")
    private BigDecimal valorMensal;

    @Min(value = 1, message = "Dia de vencimento deve estar entre 1 e 28")
    @Max(value = 28, message = "Dia de vencimento deve estar entre 1 e 28")
    @Schema(description = "Novo dia de vencimento (opcional)", example = "10")
    private Integer diaVencimento;
}
