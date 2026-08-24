package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.Recebedor;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.model.StatusAtendimento;
import com.physio.domain.model.StatusPagamento;
import com.physio.domain.model.TipoPagamento;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioPDFServiceTest {

    @Mock
    private AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Mock
    private CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;

    private RelatorioPDFService criarService() {
        RelatorioPDFService service = new RelatorioPDFService(
                atendimentoRepositoryPort, cobrancaMensalRepositoryPort, pacienteRepositoryPort);
        ReflectionTestUtils.setField(service, "nomeClinica", "Clínica Teste Fisioterapia");
        ReflectionTestUtils.setField(service, "nomeProfissional", "Dra. Fulana de Tal");
        ReflectionTestUtils.setField(service, "crefitoProfissional", "12345-F");
        return service;
    }

    private Paciente criarPaciente() {
        return Paciente.builder()
                .id(1)
                .nome("Maria da Silva")
                .dataNascimento(LocalDate.of(1990, 5, 20))
                .cpf("12345678900")
                .telefone("(11) 99999-0000")
                .build();
    }

    private ServicoConfig criarServico(String nome) {
        return ServicoConfig.builder()
                .id(1)
                .nome(nome)
                .build();
    }

    @Test
    void gerarRelatorioPersonalizado_geraPdfNaoVazioComItens() {
        Paciente paciente = criarPaciente();
        ServicoConfig servico = criarServico("Pilates");

        Atendimento atendimento = Atendimento.builder()
                .id(1)
                .paciente(paciente)
                .servicoBase(servico)
                .dataHoraInicio(LocalDateTime.of(2026, 7, 5, 10, 0))
                .valorCobrado(new BigDecimal("150.00"))
                .pctClinicaSnapshot(new BigDecimal("30"))
                .pctProfissionalSnapshot(new BigDecimal("70"))
                .status(StatusAtendimento.CONCLUIDO)
                .recebedor(Recebedor.CLINICA)
                .tipoPagamento(TipoPagamento.PIX)
                .statusPagamento(StatusPagamento.PAID)
                .build();

        when(atendimentoRepositoryPort.listarParaRelatorioFinanceiro(any(), any(), any()))
                .thenReturn(List.of(atendimento));
        when(cobrancaMensalRepositoryPort.buscarPagasPorPeriodo(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        RelatorioPDFService service = criarService();
        byte[] pdf = service.gerarRelatorioPersonalizado(
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59),
                null
        );

        assertTrue(pdf.length > 0, "PDF do acerto financeiro não deveria estar vazio");
        assertTrue(new String(pdf, 0, 5).startsWith("%PDF"), "Deveria começar com o cabeçalho de um PDF válido");
        salvarParaInspecao(pdf, "acerto-financeiro-teste.pdf");
    }

    @Test
    void gerarProntuario_geraPdfNaoVazioComEvolucoes() {
        Paciente paciente = criarPaciente();
        ServicoConfig servico = criarServico("Fisioterapia");

        Atendimento atendimento = Atendimento.builder()
                .id(2)
                .paciente(paciente)
                .servicoBase(servico)
                .dataHoraInicio(LocalDateTime.of(2026, 7, 3, 14, 30))
                .valorCobrado(BigDecimal.ZERO)
                .pctClinicaSnapshot(new BigDecimal("30"))
                .pctProfissionalSnapshot(new BigDecimal("70"))
                .status(StatusAtendimento.CONCLUIDO)
                .statusPagamento(StatusPagamento.PAYMENT_PENDING)
                .evolucao("Paciente evoluiu bem, sem queixas de dor. Manteve o protocolo.")
                .build();

        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(atendimentoRepositoryPort.listarPorPaciente(anyLong())).thenReturn(List.of(atendimento));

        RelatorioPDFService service = criarService();
        byte[] pdf = service.gerarProntuario(1L);

        assertTrue(pdf.length > 0, "PDF do prontuário não deveria estar vazio");
        assertTrue(new String(pdf, 0, 5).startsWith("%PDF"), "Deveria começar com o cabeçalho de um PDF válido");
        salvarParaInspecao(pdf, "prontuario-teste.pdf");
    }

    /** Salva o PDF gerado em target/ pra inspeção visual manual (não é asserção, só conveniência). */
    private void salvarParaInspecao(byte[] pdf, String nomeArquivo) {
        try {
            java.nio.file.Path destino = java.nio.file.Path.of("target", nomeArquivo);
            java.nio.file.Files.write(destino, pdf);
        } catch (Exception ignored) {
            // Best-effort: não falhar o teste por causa disso
        }
    }
}
