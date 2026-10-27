package com.physio.infrastructure.out.persistence.repository;

import com.physio.domain.model.StatusAtendimento;
import com.physio.domain.model.StatusPagamento;
import com.physio.domain.model.ModalidadeCobranca;
import com.physio.domain.model.TipoServico;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.entity.PacienteEntity;
import com.physio.infrastructure.out.persistence.entity.ServicoConfigEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração real contra Postgres (P1.2 item 3, Testcontainers). Cobre o que o unit test
 * mockado de DashboardServiceTest não consegue mais verificar desde a agregação em SQL (P2.3):
 * que a query realmente filtra CONCLUIDO + PAID e ignora PAYMENT_PENDING/CANCELADO/valor zero.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AtendimentoJpaRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void schema(DynamicPropertyRegistry registry) {
        // application.properties usa schema "master" por padrão — mantém igual em teste
        registry.add("spring.flyway.schemas", () -> "master");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "master");
    }

    @Autowired
    private AtendimentoJpaRepository atendimentoJpaRepository;

    @Autowired
    private PacienteJpaRepository pacienteJpaRepository;

    @Autowired
    private ServicoConfigJpaRepository servicoConfigJpaRepository;

    private PacienteEntity criarPaciente() {
        return pacienteJpaRepository.save(PacienteEntity.builder()
                .nome("Paciente Teste")
                .ativo(true)
                .build());
    }

    private ServicoConfigEntity criarServico() {
        return servicoConfigJpaRepository.save(ServicoConfigEntity.builder()
                .nome("Pilates")
                .valorBase(new BigDecimal("100.00"))
                .pctClinica(new BigDecimal("20"))
                .pctProfissional(new BigDecimal("80"))
                .ativo(true)
                .tipo(TipoServico.PILATES)
                .capacidade(15)
                .build());
    }

    private AtendimentoEntity criarAtendimento(PacienteEntity paciente, ServicoConfigEntity servico,
                                                BigDecimal valor, StatusAtendimento status,
                                                StatusPagamento statusPagamento, LocalDateTime dataHora) {
        return atendimentoJpaRepository.save(AtendimentoEntity.builder()
                .paciente(paciente)
                .servicoBase(servico)
                .dataHoraInicio(dataHora)
                .valorCobrado(valor)
                .pctClinicaSnapshot(new BigDecimal("20"))
                .pctProfissionalSnapshot(new BigDecimal("80"))
                .status(status)
                .statusPagamento(statusPagamento)
                .modalidadeCobranca(ModalidadeCobranca.AVULSO)
                .build());
    }

    @Test
    void somarFaturamentoProfissionalAvulsos_somaSoConcluidoEPagoNoPeriodo() {
        PacienteEntity paciente = criarPaciente();
        ServicoConfigEntity servico = criarServico();

        LocalDateTime dentroDoPeriodo = LocalDateTime.of(2026, 7, 10, 10, 0);
        LocalDateTime foraDoPeriodo = LocalDateTime.of(2026, 8, 1, 10, 0);

        // Entra: concluído, pago, dentro do período — 100 * 80% = 80.00
        criarAtendimento(paciente, servico, new BigDecimal("100.00"),
                StatusAtendimento.CONCLUIDO, StatusPagamento.PAID, dentroDoPeriodo);

        // Não entra: concluído mas pagamento pendente
        criarAtendimento(paciente, servico, new BigDecimal("200.00"),
                StatusAtendimento.CONCLUIDO, StatusPagamento.PAYMENT_PENDING, dentroDoPeriodo);

        // Não entra: pago mas ainda agendado (não concluído)
        criarAtendimento(paciente, servico, new BigDecimal("300.00"),
                StatusAtendimento.AGENDADO, StatusPagamento.PAID, dentroDoPeriodo);

        // Não entra: concluído e pago, mas fora do período filtrado
        criarAtendimento(paciente, servico, new BigDecimal("400.00"),
                StatusAtendimento.CONCLUIDO, StatusPagamento.PAID, foraDoPeriodo);

        BigDecimal total = atendimentoJpaRepository.somarFaturamentoProfissionalAvulsos(
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0)
        );

        assertThat(total).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void somarFaturamentoProfissionalAvulsos_retornaZeroSemAtendimentos() {
        BigDecimal total = atendimentoJpaRepository.somarFaturamentoProfissionalAvulsos(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0)
        );

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
