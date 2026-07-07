package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaTrocarPlanoRequestDTO {
    @NotNull(message = "ID do novo serviço é obrigatório")
    @Schema(example = "2")
    private Long novoServicoId;

    @NotNull(message = "Valor mensal é obrigatório")
    @Min(value = 0, message = "Valor mensal deve ser positivo")
    @Schema(example = "145.00")
    private BigDecimal novoValorMensal;

    @NotNull(message = "Dia de vencimento é obrigatório")
    @Min(value = 1, message = "Dia de vencimento deve estar entre 1 e 28")
    @Max(value = 28, message = "Dia de vencimento deve estar entre 1 e 28")
    @Schema(example = "2")
    private Integer diaVencimento;

    @Schema(description = "Data de início da nova assinatura (opcional, padrão: hoje)", example = "2026-08-01")
    private LocalDate dataInicio;
}
