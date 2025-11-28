package com.physio.domain.model;

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
public class CobrancaMensal {
    private Integer id;
    private Assinatura assinatura;
    private Integer mesReferencia;
    private Integer anoReferencia;
    private BigDecimal valor;
    private StatusCobranca status;
    private LocalDate dataPagamento;
    private Recebedor recebedor;
    private TipoPagamento tipoPagamento;
    
    // Snapshot dos percentuais para histórico financeiro
    private BigDecimal pctClinicaSnapshot;
    private BigDecimal pctProfissionalSnapshot;
    
    // Método de negócio para criar snapshot financeiro
    public void criarSnapshotFinanceiro(ServicoConfig servico) {
        this.pctClinicaSnapshot = servico.getPctClinica();
        this.pctProfissionalSnapshot = servico.getPctProfissional();
    }
}

