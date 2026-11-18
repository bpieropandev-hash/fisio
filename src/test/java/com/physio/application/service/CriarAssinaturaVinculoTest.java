package com.physio.application.service;

import com.physio.domain.model.*;
import com.physio.domain.ports.out.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CriarAssinaturaVinculoTest {
    final AssinaturaRepositoryPort assinaturas = mock(AssinaturaRepositoryPort.class);
    final PacienteRepositoryPort pacientes = mock(PacienteRepositoryPort.class);
    final ServicoRepositoryPort servicos = mock(ServicoRepositoryPort.class);
    final VincularAtendimentosAssinaturaService vincular = mock(VincularAtendimentosAssinaturaService.class);

    @Test void criarAssinaturaVinculaOQueJaEstavaAgendado() {
        var paciente = Paciente.builder().id(1).ativo(true).build();
        var servico = ServicoConfig.builder().id(2).build();
        when(servicos.buscarPorIdEAtivo(2L)).thenReturn(Optional.of(servico));
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(assinaturas.buscarAtivaPorPacienteEServico(1L, 2L)).thenReturn(Optional.empty());
        when(assinaturas.salvar(any())).thenAnswer(i -> { Assinatura a = i.getArgument(0); a.setId(7); return a; });

        var criadas = new CriarAssinaturaService(assinaturas, pacientes, servicos, vincular)
                .criarAssinatura(List.of(1L), 2L, new BigDecimal("380"), 10, LocalDate.of(2026, 11, 1));

        verify(vincular).vincular(criadas.getFirst());
    }
}
