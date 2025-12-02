package com.physio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assinatura {
    private Integer id;
    private Paciente paciente;
    private ServicoConfig servico;
    private BigDecimal valorMensal;
    private Integer diaVencimento;
    private Boolean ativo;
    private LocalDate dataInicio;
    private LocalDateTime dataCancelamento;
}
