package com.physio.application.service;

import com.physio.domain.model.Recebedor;
import com.physio.domain.model.TipoPagamento;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.entity.PacienteEntity;
import com.physio.infrastructure.out.persistence.entity.ServicoConfigEntity;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import com.physio.infrastructure.out.persistence.repository.PacienteJpaRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioPDFServiceTest {

    @Mock
    private AtendimentoJpaRepository atendimentoJpaRepository;

    @Mock
    private CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Mock
    private PacienteJpaRepository pacienteJpaRepository;

    private RelatorioPDFService criarService() {
        RelatorioPDFService service = new RelatorioPDFService(
                atendimentoJpaRepository, cobrancaMensalRepositoryPort, pacienteJpaRepository);
        ReflectionTestUtils.setField(service, "nomeClinica", "Clínica Teste Fisioterapia");
        ReflectionTestUtils.setField(service, "nomeProfissional", "Dra. Fulana de Tal");
        ReflectionTestUtils.setField(service, "crefitoProfissional", "12345-F");
        return service;
    }

    private PacienteEntity criarPaciente() {
        PacienteEntity paciente = new PacienteEntity();
        paciente.setId(1);
        paciente.setNome("Maria da Silva");
        paciente.setDataNascimento(LocalDate.of(1990, 5, 20));
        paciente.setCpf("12345678900");
        paciente.setTelefone("(11) 99999-0000");
        return paciente;
    }

    private ServicoConfigEntity criarServico(String nome) {
        ServicoConfigEntity servico = new ServicoConfigEntity();
        servico.setId(1);
        servico.setNome(nome);
        return servico;
    }

    @Test
    void gerarRelatorioPersonalizado_geraPdfNaoVazioComItens() {
        PacienteEntity paciente = criarPaciente();
        ServicoConfigEntity servico = criarServico("Pilates");

        AtendimentoEntity atendimento = new AtendimentoEntity();
        atendimento.setId(1);
        atendimento.setPaciente(paciente);
        atendimento.setServicoBase(servico);
        atendimento.setDataHoraInicio(LocalDateTime.of(2026, 7, 5, 10, 0));
        atendimento.setValorCobrado(new BigDecimal("150.00"));
        atendimento.setPctClinicaSnapshot(new BigDecimal("30"));
        atendimento.setPctProfissionalSnapshot(new BigDecimal("70"));
        atendimento.setStatus("CONCLUIDO");
        atendimento.setRecebedor(Recebedor.CLINICA);
        atendimento.setTipoPagamento(TipoPagamento.PIX);
        atendimento.setStatusPagamento("PAID");

        when(atendimentoJpaRepository.findParaRelatorioFinanceiro(any(), any(), any()))
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
        PacienteEntity paciente = criarPaciente();
        ServicoConfigEntity servico = criarServico("Fisioterapia");

        AtendimentoEntity atendimento = new AtendimentoEntity();
        atendimento.setId(2);
        atendimento.setPaciente(paciente);
        atendimento.setServicoBase(servico);
        atendimento.setDataHoraInicio(LocalDateTime.of(2026, 7, 3, 14, 30));
        atendimento.setValorCobrado(BigDecimal.ZERO);
        atendimento.setPctClinicaSnapshot(new BigDecimal("30"));
        atendimento.setPctProfissionalSnapshot(new BigDecimal("70"));
        atendimento.setStatus("CONCLUIDO");
        atendimento.setStatusPagamento("PAYMENT_PENDING");
        atendimento.setEvolucao("Paciente evoluiu bem, sem queixas de dor. Manteve o protocolo.");

        when(pacienteJpaRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(atendimentoJpaRepository.findByPaciente_Id(1)).thenReturn(List.of(atendimento));

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
