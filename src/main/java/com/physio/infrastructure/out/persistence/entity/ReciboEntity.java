package com.physio.infrastructure.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="recibos") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReciboEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=40) private String numero;
 @Column(name="atendimento_id",unique=true) private Integer atendimentoId;
 @Column(name="cobranca_id",unique=true) private Integer cobrancaId;
 @Column(nullable=false) private String pacienteNome;
 private String pacienteDocumento;
 @Column(nullable=false) private String pagadorNome;
 @Column(nullable=false,length=30) private String pagadorDocumento;
 @Column(nullable=false) private String emitenteNome;
 @Column(nullable=false,length=30) private String emitenteDocumento;
 @Column(nullable=false,length=1000) private String descricao;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal valor;
 @Column(nullable=false) private LocalDate dataPagamento;
 @Column(length=25) private String tipoPagamento;
 @Column(nullable=false) private LocalDateTime emitidoEm;
}
