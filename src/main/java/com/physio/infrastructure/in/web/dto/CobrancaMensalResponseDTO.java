package com.physio.infrastructure.in.web.dto;

import com.physio.domain.model.Recebedor;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.model.TipoPagamento;
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
public class CobrancaMensalResponseDTO {
    @Schema(example = "1")
    private Integer id;

    @Schema(example = "1")
    private Integer assinaturaId;

    @Schema(example = "João da Silva - Pilates")
    private String descricao;

    @Schema(example = "11")
    private Integer mesReferencia;

    @Schema(example = "2025")
    private Integer anoReferencia;

    @Schema(example = "300.00")
    private BigDecimal valor;

    @Schema(example = "PENDENTE")
    private StatusCobranca status;

    @Schema(example = "2025-11-15")
    private LocalDate dataPagamento;

    private Recebedor recebedor;

    private TipoPagamento tipoPagamento;

    @Schema(example = "20.00")
    private BigDecimal pctClinicaSnapshot;

    @Schema(example = "80.00")
    private BigDecimal pctProfissionalSnapshot;
}

