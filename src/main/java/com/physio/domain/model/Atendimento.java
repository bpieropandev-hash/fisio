package com.physio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Atendimento {
    private Integer id;
    private Paciente paciente;
    private ServicoConfig servicoBase;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private String status;
    private BigDecimal valorCobrado;
    private BigDecimal pctClinicaSnapshot;
    private BigDecimal pctProfissionalSnapshot;
    private String evolucao;
    
    // Método de negócio para criar snapshot financeiro
    public void criarSnapshotFinanceiro(ServicoConfig servico) {
        this.valorCobrado = servico.getValorBase();
        this.pctClinicaSnapshot = servico.getPctClinica();
        this.pctProfissionalSnapshot = servico.getPctProfissional();
    }
}

