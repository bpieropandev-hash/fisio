package com.physio.infrastructure.in.web.dto;

import com.physio.domain.model.TipoServico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoResponseDTO {
    private Integer id;

    @Schema(example = "Fisioterapia (Domicílio)")
    private String nome;
    private BigDecimal valorBase;
    private BigDecimal pctClinica;
    private BigDecimal pctProfissional;
    private Boolean ativo;
    private TipoServico tipo;
}
