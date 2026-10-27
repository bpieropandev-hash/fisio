package com.physio.application.service;

import com.physio.domain.model.*;
import com.physio.domain.ports.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstadoAtivoOperacoesTest {
    private final AtendimentoRepositoryPort atendimentos = mock(AtendimentoRepositoryPort.class);
    private final PacienteRepositoryPort pacientes = mock(PacienteRepositoryPort.class);
    private final ServicoRepositoryPort servicos = mock(ServicoRepositoryPort.class);
    private final AssinaturaRepositoryPort assinaturas = mock(AssinaturaRepositoryPort.class);
    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 27, 10, 0);

    private Paciente paciente(Boolean ativo) {
        return Paciente.builder().id(1).nome("Paciente").ativo(ativo).build();
    }

    private ServicoConfig servico(Boolean ativo) {
        return ServicoConfig.builder().id(2).nome("Pilates").tipo(TipoServico.PILATES).ativo(ativo)
                .valorBase(BigDecimal.TEN).pctClinica(new BigDecimal("20"))
                .pctProfissional(new BigDecimal("80")).build();
    }

    @Test
    void agendamentoRejeitaPacienteInativoSemSalvar() {
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente(false)));
        when(servicos.buscarPorIdEAtivo(2L)).thenReturn(Optional.of(servico(true)));
        when(atendimentos.salvar(any())).thenAnswer(i -> i.getArgument(0));

        var agenda = new RealizarAgendamentoService(atendimentos, pacientes, servicos, assinaturas);
        assertThrows(IllegalArgumentException.class,
                () -> agenda.realizarAgendamento(1L, 2L, inicio, null, null, 1));
        verify(atendimentos, never()).salvar(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = true)
    void agendamentoPermitePacienteAtivoOuLegado(Boolean ativo) {
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente(ativo)));
        when(servicos.buscarPorIdEAtivo(2L)).thenReturn(Optional.of(servico(true)));
        when(atendimentos.salvar(any())).thenAnswer(i -> i.getArgument(0));

        var agenda = new RealizarAgendamentoService(atendimentos, pacientes, servicos, assinaturas);
        assertEquals(1, agenda.realizarAgendamento(1L, 2L, inicio, null, null, 1).size());
    }

    @ParameterizedTest
    @CsvSource({"false,true,false", "true,false,false", "false,true,true", "true,false,true"})
    void reativacaoRejeitaRelacionamentoInativoInclusiveAssinaturaJaAtiva(
            boolean pacienteAtivo, boolean servicoAtivo, boolean assinaturaAtiva) {
        var assinatura = Assinatura.builder().id(3).paciente(paciente(pacienteAtivo))
                .servico(servico(servicoAtivo)).ativo(assinaturaAtiva).build();
        when(assinaturas.buscarPorId(3L)).thenReturn(Optional.of(assinatura));
        when(assinaturas.salvar(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(IllegalArgumentException.class,
                () -> new ReativarAssinaturaService(assinaturas).reativar(3L));
        assertEquals(assinaturaAtiva, assinatura.getAtivo());
        verify(assinaturas, never()).salvar(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = true)
    void reativacaoPermiteRelacionamentosAtivosOuLegados(Boolean ativo) {
        var assinatura = Assinatura.builder().id(3).paciente(paciente(ativo))
                .servico(servico(ativo)).ativo(false).dataCancelamento(inicio).build();
        when(assinaturas.buscarPorId(3L)).thenReturn(Optional.of(assinatura));
        when(assinaturas.salvar(any())).thenAnswer(i -> i.getArgument(0));

        var reativada = new ReativarAssinaturaService(assinaturas).reativar(3L);
        assertTrue(reativada.getAtivo());
        assertNull(reativada.getDataCancelamento());
        verify(assinaturas).salvar(assinatura);
    }

    @Test
    void reativacaoJaAtivaValidaPermaneceIdempotente() {
        var assinatura = Assinatura.builder().id(3).paciente(paciente(true))
                .servico(servico(true)).ativo(true).build();
        when(assinaturas.buscarPorId(3L)).thenReturn(Optional.of(assinatura));

        assertSame(assinatura, new ReativarAssinaturaService(assinaturas).reativar(3L));
        verify(assinaturas, never()).salvar(any());
    }
}
