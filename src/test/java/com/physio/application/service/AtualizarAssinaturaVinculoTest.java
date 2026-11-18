package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AtualizarAssinaturaVinculoTest {
    final AssinaturaRepositoryPort assinaturas = mock(AssinaturaRepositoryPort.class);
    final VincularAtendimentosAssinaturaService vincular = mock(VincularAtendimentosAssinaturaService.class);
    final AtualizarAssinaturaService service = new AtualizarAssinaturaService(assinaturas, vincular);

    @Test void editarAssinaturaAtivaVinculaOQueAindaEstaAvulso() {
        var assinatura = Assinatura.builder().id(5).ativo(true).build();
        when(assinaturas.buscarPorId(5L)).thenReturn(Optional.of(assinatura));
        when(assinaturas.salvar(any())).thenAnswer(i -> i.getArgument(0));
        service.atualizar(5L, new BigDecimal("400"), 12);
        verify(vincular).vincular(assinatura);
    }

    @Test void assinaturaCanceladaNaoEditaNemVincula() {
        when(assinaturas.buscarPorId(6L)).thenReturn(Optional.of(Assinatura.builder().id(6).ativo(false).build()));
        try { service.atualizar(6L, null, null); } catch (IllegalArgumentException esperado) { /* regra existente */ }
        verifyNoInteractions(vincular);
    }
}
