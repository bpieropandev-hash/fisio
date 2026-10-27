package com.physio.infrastructure.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name="evolucoes_clinicas",uniqueConstraints=@UniqueConstraint(columnNames={"atendimento_id","versao"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EvolucaoClinicaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="atendimento_id",nullable=false) private Integer atendimentoId;
    @Column(nullable=false) private Integer versao;
    @Column(nullable=false,length=300) private String procedimentos;
    @Column(nullable=false,length=150) private String equipamentos;
    @Column(name="pressao_sistolica") private Integer pressaoSistolica;
    @Column(name="pressao_diastolica") private Integer pressaoDiastolica;
    private Integer dor;
    @Column(nullable=false,length=5000) private String observacoes;
    @Column(nullable=false,columnDefinition="text") private String resumo;
    @Column(name="texto_legado",nullable=false,columnDefinition="text") private String textoLegado;
    @Column(nullable=false,length=255) private String autor;
    @Column(name="atualizado_em",nullable=false) private LocalDateTime atualizadoEm;
}
