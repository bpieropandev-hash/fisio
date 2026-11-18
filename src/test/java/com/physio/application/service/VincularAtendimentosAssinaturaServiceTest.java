package com.physio.application.service;

import com.physio.domain.model.*;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Ao criar a assinatura, o que já estava agendado como avulso a partir do início passa a ser mensalidade. */
class VincularAtendimentosAssinaturaServiceTest {
    final AtendimentoRepositoryPort atendimentos = mock(AtendimentoRepositoryPort.class);
    final VincularAtendimentosAssinaturaService service = new VincularAtendimentosAssinaturaService(atendimentos);
    final Paciente paciente = Paciente.builder().id(1).nome("João").build();
    final ServicoConfig servico = ServicoConfig.builder().id(2).nome("Pilates").tipo(TipoServico.PILATES)
            .valorBase(new BigDecimal("100")).pctClinica(new BigDecimal("20")).pctProfissional(new BigDecimal("80")).build();
    final Assinatura assinatura = Assinatura.builder().id(9).paciente(paciente).servico(servico).ativo(true)
            .dataInicio(LocalDate.of(2026, 11, 1)).build();

    Atendimento atendimento(int id, LocalDateTime inicio, StatusAtendimento status, ModalidadeCobranca modalidade,
                            StatusPagamento pagamento, Integer assinaturaId) {
        return Atendimento.builder().id(id).paciente(paciente).servicoBase(servico).dataHoraInicio(inicio).dataHoraFim(inicio.plusHours(1))
                .status(status).modalidadeCobranca(modalidade).statusPagamento(pagamento).assinaturaId(assinaturaId)
                .valorCobrado(new BigDecimal("100")).pctClinicaSnapshot(new BigDecimal("20")).pctProfissionalSnapshot(new BigDecimal("80")).build();
    }

    void candidatos(Atendimento... lista) {
        when(atendimentos.candidatosRemarcacao(eq(1L), eq(2L), any(), any())).thenReturn(List.of(lista));
        when(atendimentos.salvar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void convertePendentesAPartirDoInicioInclusiveOPrimeiroDia() {
        var primeiroDia = atendimento(1, LocalDateTime.of(2026, 11, 1, 8, 0), StatusAtendimento.AGENDADO, ModalidadeCobranca.AVULSO, StatusPagamento.PAYMENT_PENDING, null);
        var futuro = atendimento(2, LocalDateTime.of(2026, 11, 20, 9, 0), StatusAtendimento.AGENDADO, ModalidadeCobranca.AVULSO, StatusPagamento.PAYMENT_PENDING, null);
        candidatos(primeiroDia, futuro);

        assertEquals(2, service.vincular(assinatura));

        for (var a : List.of(primeiroDia, futuro)) {
            assertEquals(9, a.getAssinaturaId());
            assertEquals(ModalidadeCobranca.MENSALIDADE, a.getModalidadeCobranca());
            assertEquals(0, BigDecimal.ZERO.compareTo(a.getValorCobrado()));
            assertEquals(0, BigDecimal.ZERO.compareTo(a.getPctClinicaSnapshot()));
            assertEquals(0, BigDecimal.ZERO.compareTo(a.getPctProfissionalSnapshot()));
        }
        verify(atendimentos, times(2)).salvar(any());
    }

    @Test void consultaSoDeOInicioDaVigenciaEmDiante() {
        candidatos();
        service.vincular(assinatura);
        verify(atendimentos).candidatosRemarcacao(eq(1L), eq(2L), eq(LocalDateTime.of(2026, 11, 1, 0, 0)), any());
    }

    @Test void naoTocaEmAtendimentoAnteriorAoInicioMesmoSeVierNaLista() {
        var antigo = atendimento(3, LocalDateTime.of(2026, 10, 20, 9, 0), StatusAtendimento.AGENDADO, ModalidadeCobranca.AVULSO, StatusPagamento.PAYMENT_PENDING, null);
        candidatos(antigo);
        assertEquals(0, service.vincular(assinatura));
        assertEquals(ModalidadeCobranca.AVULSO, antigo.getModalidadeCobranca());
        assertEquals(0, new BigDecimal("100").compareTo(antigo.getValorCobrado()));
        verify(atendimentos, never()).salvar(any());
    }

    @Test void concluidoComPagamentoPendenteViraMensalidade() {
        var concluido = atendimento(4, LocalDateTime.of(2026, 11, 5, 9, 0), StatusAtendimento.CONCLUIDO, ModalidadeCobranca.AVULSO, StatusPagamento.PAYMENT_PENDING, null);
        candidatos(concluido);
        assertEquals(1, service.vincular(assinatura));
        assertEquals(ModalidadeCobranca.MENSALIDADE, concluido.getModalidadeCobranca());
    }

    @Test void preservaOQueJaFoiPagoCanceladoFaltaLegadoOuJaCoberto() {
        var data = LocalDateTime.of(2026, 11, 6, 9, 0);
        var pago = atendimento(5, data, StatusAtendimento.CONCLUIDO, ModalidadeCobranca.AVULSO, StatusPagamento.PAID, null);
        var cancelado = atendimento(6, data.plusDays(1), StatusAtendimento.CANCELADO, ModalidadeCobranca.AVULSO, StatusPagamento.PAYMENT_PENDING, null);
        var falta = atendimento(7, data.plusDays(2), StatusAtendimento.FALTA, ModalidadeCobranca.AVULSO, StatusPagamento.PAYMENT_PENDING, null);
        var legado = atendimento(8, data.plusDays(3), StatusAtendimento.AGENDADO, ModalidadeCobranca.LEGADO, StatusPagamento.PAYMENT_PENDING, null);
        var outraAssinatura = atendimento(10, data.plusDays(4), StatusAtendimento.AGENDADO, ModalidadeCobranca.MENSALIDADE, StatusPagamento.PAYMENT_PENDING, 3);
        candidatos(pago, cancelado, falta, legado, outraAssinatura);

        assertEquals(0, service.vincular(assinatura));
        assertEquals(ModalidadeCobranca.AVULSO, pago.getModalidadeCobranca());
        assertEquals(0, new BigDecimal("100").compareTo(pago.getValorCobrado()));
        assertEquals(3, outraAssinatura.getAssinaturaId());
        verify(atendimentos, never()).salvar(any());
    }

    @Test void respeitaOFimDaVigenciaQuandoHaCancelamento() {
        var cancelamento = LocalDateTime.of(2026, 11, 15, 0, 0);
        var comFim = Assinatura.builder().id(9).paciente(paciente).servico(servico).ativo(false)
                .dataInicio(LocalDate.of(2026, 11, 1)).dataCancelamento(cancelamento).build();
        candidatos();
        service.vincular(comFim);
        verify(atendimentos).candidatosRemarcacao(eq(1L), eq(2L), any(), eq(cancelamento));
    }

    @Test void assinaturaSemInicioOuSemIdNaoFazNada() {
        assertEquals(0, service.vincular(Assinatura.builder().id(9).paciente(paciente).servico(servico).build()));
        assertEquals(0, service.vincular(Assinatura.builder().paciente(paciente).servico(servico).dataInicio(LocalDate.now()).build()));
        verifyNoInteractions(atendimentos);
    }
}
