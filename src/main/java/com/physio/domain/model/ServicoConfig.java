package com.physio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.physio.domain.model.TipoServico;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoConfig {
    private Integer id;
    private String nome;
    private BigDecimal valorBase;
    private BigDecimal pctClinica;
    private BigDecimal pctProfissional;
    private Boolean ativo;
    private TipoServico tipo; // Novo campo: tipo do serviço (PILATES ou FISIOTERAPIA)
}
