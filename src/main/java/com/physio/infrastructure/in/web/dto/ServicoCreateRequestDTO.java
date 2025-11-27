package com.physio.infrastructure.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoCreateRequestDTO {
    @NotBlank
    @Schema(description = "Nome do serviço", example = "Fisioterapia (Domicílio)")
    private String nome;

    @NotNull
    @Schema(description = "Valor base do serviço", example = "200.00")
    private BigDecimal valorBase;

    @NotNull
    @Schema(description = "% destinado à clínica", example = "20.00")
    private BigDecimal pctClinica;

    @NotNull
    @Schema(description = "% destinado ao profissional", example = "80.00")
    private BigDecimal pctProfissional;

    private Boolean ativo;
}
