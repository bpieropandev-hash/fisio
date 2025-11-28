package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
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
public class AssinaturaCreateRequestDTO {
    @NotNull(message = "ID do paciente é obrigatório")
    @Schema(example = "1")
    private Long pacienteId;

    @NotNull(message = "ID do serviço é obrigatório")
    @Schema(example = "2")
    private Long servicoId;

    @NotNull(message = "Valor mensal é obrigatório")
    @Min(value = 0, message = "Valor mensal deve ser positivo")
    @Schema(description = "Valor da mensalidade", example = "300.00")
    private BigDecimal valorMensal;

    @NotNull(message = "Dia de vencimento é obrigatório")
    @Min(value = 1, message = "Dia de vencimento deve estar entre 1 e 28")
    @Max(value = 28, message = "Dia de vencimento deve estar entre 1 e 28")
    @Schema(description = "Dia do mês em que a mensalidade vence", example = "5")
    private Integer diaVencimento;

    @Schema(description = "Data de início da assinatura (opcional, padrão: hoje)", example = "2025-01-01")
    private LocalDate dataInicio;
}

