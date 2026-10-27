package com.physio.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="configuracoes_clinica")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConfiguracoesEntity {
    @Id private Integer id;
    @Column(nullable=false) private Long versao;
    @Column(columnDefinition="text") private String configuracao;
}
