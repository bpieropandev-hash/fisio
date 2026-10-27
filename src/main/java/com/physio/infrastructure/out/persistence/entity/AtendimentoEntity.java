package com.physio.infrastructure.out.persistence.entity;

import com.physio.domain.model.Recebedor;
import com.physio.domain.model.StatusAtendimento;
import com.physio.domain.model.StatusPagamento;
import com.physio.domain.model.TipoPagamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "atendimentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtendimentoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PacienteEntity paciente;

    @OneToOne
    @JoinColumn(name = "servico_base_id", nullable = false)
    private ServicoConfigEntity servicoBase;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "valor_cobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorCobrado;

    @Column(name = "pct_clinica_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctClinicaSnapshot;

    @Column(name = "pct_profissional_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctProfissionalSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtendimento status;

    @Column(name = "data_hora_fim")
    private LocalDateTime dataHoraFim;

    @Column(name="evolucao", columnDefinition = "TEXT")
    private String evolucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "recebedor", length = 10) // Opcional: define tamanho igual ao banco
    private Recebedor recebedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", length = 25)
    private TipoPagamento tipoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", length = 20, nullable = false)
    private StatusPagamento statusPagamento;

    @Column(name = "assinatura_id")
    private Integer assinaturaId;
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_cobranca", nullable = false, length = 20)
    @Builder.Default
    private com.physio.domain.model.ModalidadeCobranca modalidadeCobranca = com.physio.domain.model.ModalidadeCobranca.LEGADO;
    @Column(name = "data_pagamento")
    private java.time.LocalDate dataPagamento;
    public PacienteEntity getPaciente() {
        return paciente;
    }

    public void setPaciente(PacienteEntity paciente) {
        this.paciente = paciente;
    }

    @Transient
    public Integer getPacienteId() {
        return this.paciente != null ? this.paciente.getId() : null;
    }

}

