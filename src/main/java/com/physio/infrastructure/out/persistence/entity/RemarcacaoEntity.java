package com.physio.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="remarcacao_operacoes") @Data @NoArgsConstructor
public class RemarcacaoEntity {
 @Id private String id;
 @Column(nullable=false) private String autor;
 @Column(nullable=false) private String motivo;
 @Column(nullable=false) private LocalDateTime criadaEm;
 private LocalDateTime confirmadaEm;
 @Column(nullable=false, columnDefinition="text") private String pedido;
 @Column(nullable=false, columnDefinition="text") private String assinatura;
 @Column(nullable=false, columnDefinition="text") private String previa;
 @Column(columnDefinition="text") private String resultado;
}
