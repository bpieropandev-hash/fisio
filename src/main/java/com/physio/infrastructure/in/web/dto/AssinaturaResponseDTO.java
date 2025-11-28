package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class AssinaturaResponseDTO {
    @Schema(example = "1")
    private Integer id;

    @Schema(example = "1")
    private Integer pacienteId;

    @Schema(example = "João da Silva")
    private String pacienteNome;

    @Schema(example = "2")
    private Integer servicoId;

    @Schema(example = "Pilates")
    private String servicoNome;

    @Schema(example = "300.00")
    private BigDecimal valorMensal;

    @Schema(example = "5")
    private Integer diaVencimento;

    @Schema(example = "true")
    private Boolean ativo;

    @Schema(example = "2025-01-01")
    private LocalDate dataInicio;
}

