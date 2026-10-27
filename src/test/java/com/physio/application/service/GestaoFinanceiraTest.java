package com.physio.application.service;

import com.physio.domain.model.*;
import com.physio.domain.ports.out.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestaoFinanceiraTest {
    final AtendimentoRepositoryPort atendimentos = mock(AtendimentoRepositoryPort.class);
    final PacienteRepositoryPort pacientes = mock(PacienteRepositoryPort.class);
    final ServicoRepositoryPort servicos = mock(ServicoRepositoryPort.class);
    final AssinaturaRepositoryPort assinaturas = mock(AssinaturaRepositoryPort.class);
    final LocalDateTime inicio = LocalDateTime.of(2026, 10, 26, 10, 0);
    final Paciente paciente = Paciente.builder().id(1).nome("Paciente").build();
    final ServicoConfig servico = ServicoConfig.builder().id(1).nome("Pilates").tipo(TipoServico.PILATES)
            .valorBase(new BigDecimal("100")).pctClinica(new BigDecimal("20")).pctProfissional(new BigDecimal("80")).build();

    RealizarAgendamentoService agenda() {
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(servicos.buscarPorIdEAtivo(1L)).thenReturn(Optional.of(servico));
        when(atendimentos.salvar(any())).thenAnswer(i -> i.getArgument(0));
        return new RealizarAgendamentoService(atendimentos, pacientes, servicos, assinaturas);
    }

    @Test void assinaturaFuturaNaoCobreSessaoAnteriorAoInicio() {
        var agenda = agenda();
        when(assinaturas.buscarAtivaPorPacienteEServico(1L, 1L)).thenReturn(Optional.of(Assinatura.builder()
                .id(9).ativo(true).dataInicio(LocalDate.of(2026, 11, 1)).build()));
        assertEquals(new BigDecimal("100"), agenda.realizarAgendamento(1L, 1L, inicio, null, null, 1).getFirst().getValorCobrado());
    }

    @Test void pacienteNaoPodeOcuparDuasVagasSobrepostas() {
        var agenda = agenda();
        when(atendimentos.listarConflitosPorPeriodo(any(), any())).thenReturn(List.of(Atendimento.builder()
                .id(8).paciente(paciente).servicoBase(servico).status(StatusAtendimento.AGENDADO).build()));
        assertThrows(IllegalArgumentException.class, () -> agenda.realizarAgendamento(1L, 1L, inicio, null, null, 1));
    }

    @Test void recorrenciaNaoDeveCriarParcialmenteSeAlgumaDataConflita() {
        var agenda = agenda();
        servico.setTipo(TipoServico.FISIOTERAPIA);
        when(atendimentos.listarConflitosPorPeriodo(any(), any())).thenReturn(List.of()).thenReturn(List.of(Atendimento.builder()
                .id(8).paciente(paciente).servicoBase(servico).status(StatusAtendimento.AGENDADO).build()));
        assertThrows(IllegalArgumentException.class, () -> agenda.realizarAgendamento(1L, 1L, inicio, inicio.toLocalDate().plusWeeks(1), null, 1));
        verify(atendimentos, never()).salvar(any());
    }

    @Test void naoGeraCompetenciaAnteriorAoInicioDaAssinatura() {
        var cobrancas = mock(CobrancaMensalRepositoryPort.class);
        when(assinaturas.listarAtivas()).thenReturn(List.of(Assinatura.builder().id(1).servico(servico)
                .valorMensal(BigDecimal.TEN).dataInicio(LocalDate.of(2026, 11, 15)).build()));
        when(cobrancas.buscarAssinaturaIdsComCobranca(10, 2026)).thenReturn(Set.of());
        assertEquals(0, new GeradorCobrancaService(assinaturas, cobrancas).gerarCobrancasDoMes(10, 2026));
        verify(cobrancas, never()).salvar(any());
    }

    @Test void capacidadeConfiguradaSubstituiLimiteFixoEPacienteCanceladoNaoOcupaVaga() {
        var agenda = agenda(); servico.setCapacidade(1);
        var outro = Atendimento.builder().id(8).paciente(Paciente.builder().id(2).build()).servicoBase(servico).status(StatusAtendimento.AGENDADO).build();
        when(atendimentos.listarConflitosPorPeriodo(any(), any())).thenReturn(List.of(outro));
        assertThrows(IllegalArgumentException.class, () -> agenda.realizarAgendamento(1L, 1L, inicio, null, null, 1));
        outro.setStatus(StatusAtendimento.CANCELADO);
        assertEquals(1, agenda.realizarAgendamento(1L, 1L, inicio, null, null, 1).size());
    }

    @Test void valorZeroAvulsoNaoViraMensalidade() {
        var agenda=agenda(); servico.setValorBase(BigDecimal.ZERO);
        var salvo=agenda.realizarAgendamento(1L,1L,inicio,null,null,1).getFirst();
        assertEquals(ModalidadeCobranca.AVULSO,salvo.getModalidadeCobranca());
        assertNull(salvo.getAssinaturaId());
    }

    @Test void assinaturaVigenteVinculaCoberturaSemCobrarPorSessao() {
        var agenda=agenda();
        when(assinaturas.buscarAtivaPorPacienteEServico(1L,1L)).thenReturn(Optional.of(Assinatura.builder().id(3).ativo(true).dataInicio(inicio.toLocalDate()).build()));
        var salvo=agenda.realizarAgendamento(1L,1L,inicio,null,null,1).getFirst();
        assertEquals(ModalidadeCobranca.MENSALIDADE,salvo.getModalidadeCobranca());
        assertEquals(3,salvo.getAssinaturaId()); assertEquals(BigDecimal.ZERO,salvo.getValorCobrado());
    }

    Atendimento existente() {
        var a=Atendimento.builder().id(8).paciente(paciente).servicoBase(servico).dataHoraInicio(inicio).dataHoraFim(inicio.plusHours(1))
                .modalidadeCobranca(ModalidadeCobranca.AVULSO).status(StatusAtendimento.AGENDADO).statusPagamento(StatusPagamento.PAYMENT_PENDING).valorCobrado(BigDecimal.TEN).build();
        when(atendimentos.buscarPorId(8L)).thenReturn(a);
        when(atendimentos.salvar(any())).thenAnswer(i -> i.getArgument(0));
        return a;
    }

    @Test void remarcacaoRespeitaCapacidadeConfigurada() {
        existente(); servico.setCapacidade(1);
        when(atendimentos.listarConflitosPorPeriodo(any(),any())).thenReturn(List.of(Atendimento.builder().id(9).paciente(Paciente.builder().id(2).build()).servicoBase(servico).status(StatusAtendimento.AGENDADO).build()));
        assertThrows(IllegalArgumentException.class,()->new AtualizarAtendimentoService(atendimentos,assinaturas).atualizar(8L,Atendimento.builder().dataHoraInicio(inicio.plusHours(2)).build()));
        verify(atendimentos,never()).salvar(any());
    }

    @Test void novoRecebimentoExigeDataExplicita() {
        existente();
        var dados=Atendimento.builder().recebedor(Recebedor.CLINICA).tipoPagamento(TipoPagamento.PIX).build();
        assertThrows(IllegalArgumentException.class,()->new AtualizarAtendimentoService(atendimentos,assinaturas).atualizar(8L,dados));
        verify(atendimentos,never()).salvar(any());
    }

    @Test void pagamentoComDataPermiteCortesiaSemInferirAssinatura() {
        var atual=existente(); atual.setValorCobrado(BigDecimal.ZERO);
        var salvo=new AtualizarAtendimentoService(atendimentos,assinaturas).atualizar(8L,Atendimento.builder().recebedor(Recebedor.CLINICA).tipoPagamento(TipoPagamento.PIX).dataPagamento(LocalDate.now()).build());
        assertEquals(StatusPagamento.PAID,salvo.getStatusPagamento());
        assertEquals(ModalidadeCobranca.AVULSO,salvo.getModalidadeCobranca());
    }

    @Test void evolucaoEstruturadaNaoPodeSerSobrescritaPeloPutLegado() {
        var atual=existente(); atual.setEvolucao("Resumo estruturado");
        when(atendimentos.possuiEvolucaoEstruturada(8L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,()->new AtualizarAtendimentoService(atendimentos,assinaturas).atualizar(8L,Atendimento.builder().evolucao("Texto divergente").build()));
        verify(atendimentos,never()).salvar(any());
    }

    @Test void mensalidadeNaoRecebePagamentoAvulso() {
        var atual=existente(); atual.setAssinaturaId(3); atual.setModalidadeCobranca(ModalidadeCobranca.MENSALIDADE);
        assertThrows(IllegalArgumentException.class,()->new AtualizarAtendimentoService(atendimentos,assinaturas).atualizar(8L,Atendimento.builder().recebedor(Recebedor.CLINICA).tipoPagamento(TipoPagamento.PIX).dataPagamento(LocalDate.now()).build()));
    }
}
