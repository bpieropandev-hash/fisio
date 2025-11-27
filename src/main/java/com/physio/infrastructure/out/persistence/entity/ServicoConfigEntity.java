package com.physio.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Entity
@Table(name = "servicos_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "valor_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorBase;

    @Column(name = "pct_clinica", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctClinica;

    @Column(name = "pct_profissional", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctProfissional;

    @Column(nullable = false)
    private Boolean ativo;
}

