package com.physio.infrastructure.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name="avaliacoes_fisicas")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AvaliacaoFisicaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="paciente_id",nullable=false) private Integer pacienteId;
    @Column(name="data_avaliacao",nullable=false) private LocalDate dataAvaliacao;
    @Column(nullable=false,length=120) private String titulo;
    @Column(nullable=false,length=2000) private String observacao;
    @Column(name="criado_em",nullable=false) private LocalDateTime criadoEm;
    @Column(nullable=false,length=255) private String autor;
    @Column(name="nome_arquivo",nullable=false,length=255) private String nomeArquivo;
    @Column(name="tipo_conteudo",nullable=false,length=20) private String tipoConteudo;
    @Column(nullable=false,columnDefinition="bytea") private byte[] conteudo;
}
