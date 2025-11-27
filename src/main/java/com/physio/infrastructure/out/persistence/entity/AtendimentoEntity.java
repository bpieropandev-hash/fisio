package com.physio.infrastructure.out.persistence.entity;

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

    @Column(nullable = false)
    private String status;

    @Column(name = "data_hora_fim")
    private LocalDateTime dataHoraFim;

    @Column(name="evolucao", columnDefinition = "TEXT")
    private String evolucao;

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

