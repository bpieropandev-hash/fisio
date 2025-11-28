package com.physio.infrastructure.out.persistence.entity;

import com.physio.domain.model.Recebedor;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.model.TipoPagamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cobrancas_mensais")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CobrancaMensalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private AssinaturaEntity assinatura;

    @Column(name = "mes_referencia", nullable = false)
    private Integer mesReferencia;

    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusCobranca status;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Recebedor recebedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", length = 25)
    private TipoPagamento tipoPagamento;

    @Column(name = "pct_clinica_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctClinicaSnapshot;

    @Column(name = "pct_profissional_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctProfissionalSnapshot;
}

