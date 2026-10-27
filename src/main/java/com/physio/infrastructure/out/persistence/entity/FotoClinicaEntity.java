package com.physio.infrastructure.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name="fotos_clinicas")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FotoClinicaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="paciente_id",nullable=false) private Integer pacienteId;
    @Column(name="data_captura",nullable=false) private LocalDate dataCaptura;
    @Column(nullable=false,length=100) private String regiao;
    @Column(nullable=false,length=2000) private String observacao;
    @Column(name="criado_em",nullable=false) private LocalDateTime criadoEm;
    @Column(nullable=false,length=255) private String autor;
    @Column(name="tipo_conteudo",nullable=false,length=20) private String tipoConteudo;
    @Column(nullable=false,columnDefinition="bytea") private byte[] conteudo;
}
