package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GerarCobrancasRequestDTO {
    @NotNull(message = "Mês é obrigatório")
    @Min(value = 1, message = "Mês deve estar entre 1 e 12")
    @Max(value = 12, message = "Mês deve estar entre 1 e 12")
    @Schema(description = "Mês de referência", example = "11")
    private Integer mes;

    @NotNull(message = "Ano é obrigatório")
    @Min(value = 2020, message = "Ano inválido")
    @Schema(description = "Ano de referência", example = "2025")
    private Integer ano;
}

