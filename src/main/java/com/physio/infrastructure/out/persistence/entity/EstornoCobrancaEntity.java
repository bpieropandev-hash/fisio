package com.physio.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estornos_cobranca")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class EstornoCobrancaEntity {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(name="cobranca_id", nullable=false) private Integer cobrancaId;
 @Column(nullable=false, length=1000) private String motivo;
 @Column(nullable=false) private String autor;
 @Column(name="estornado_em", nullable=false) private LocalDateTime estornadoEm;
 @Column(name="data_pagamento_anterior") private LocalDate dataPagamentoAnterior;
 @Column(name="recebedor_anterior") private String recebedorAnterior;
 @Column(name="tipo_pagamento_anterior") private String tipoPagamentoAnterior;
}
