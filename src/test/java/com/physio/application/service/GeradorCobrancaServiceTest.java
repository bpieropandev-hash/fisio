package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeradorCobrancaServiceTest {

    @Mock
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Mock
    private CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    private GeradorCobrancaService geradorCobrancaService;

    @BeforeEach
    void setUp() {
        geradorCobrancaService = new GeradorCobrancaService(assinaturaRepositoryPort, cobrancaMensalRepositoryPort);
    }

    private Assinatura criarAssinatura(int id, String valor) {
        ServicoConfig servico = ServicoConfig.builder()
                .id(1)
                .pctClinica(new BigDecimal("20"))
                .pctProfissional(new BigDecimal("80"))
                .build();
        return Assinatura.builder()
                .id(id)
                .servico(servico)
                .valorMensal(new BigDecimal(valor))
                .ativo(true)
                .build();
    }

    @Test
    void naoDeveGerarCobrancaDuplicadaParaAssinaturaJaCobradaNoMes() {
        Assinatura assinatura = criarAssinatura(1, "180.00");
        when(assinaturaRepositoryPort.listarAtivas()).thenReturn(List.of(assinatura));
        when(cobrancaMensalRepositoryPort.buscarPorAssinaturaMesAno(1L, 7, 2026))
                .thenReturn(Optional.of(CobrancaMensal.builder().id(99).build()));

        int geradas = geradorCobrancaService.gerarCobrancasDoMes(7, 2026);

        assertEquals(0, geradas);
        verify(cobrancaMensalRepositoryPort, never()).salvar(any());
    }

    @Test
    void deveGerarCobrancaComSnapshotDePercentualEValorDaAssinatura() {
        Assinatura assinatura = criarAssinatura(2, "180.00");
        when(assinaturaRepositoryPort.listarAtivas()).thenReturn(List.of(assinatura));
        when(cobrancaMensalRepositoryPort.buscarPorAssinaturaMesAno(2L, 7, 2026))
                .thenReturn(Optional.empty());

        int geradas = geradorCobrancaService.gerarCobrancasDoMes(7, 2026);

        assertEquals(1, geradas);

        ArgumentCaptor<CobrancaMensal> captor = ArgumentCaptor.forClass(CobrancaMensal.class);
        verify(cobrancaMensalRepositoryPort).salvar(captor.capture());

        CobrancaMensal salva = captor.getValue();
        assertEquals(new BigDecimal("180.00"), salva.getValor());
        assertEquals(new BigDecimal("20"), salva.getPctClinicaSnapshot());
        assertEquals(new BigDecimal("80"), salva.getPctProfissionalSnapshot());
    }

    @Test
    void deveContinuarProcessandoDemaisAssinaturasQuandoUmaFalhaPorCorridaConcorrente() {
        Assinatura assinatura1 = criarAssinatura(3, "100.00");
        Assinatura assinatura2 = criarAssinatura(4, "200.00");
        when(assinaturaRepositoryPort.listarAtivas()).thenReturn(List.of(assinatura1, assinatura2));
        when(cobrancaMensalRepositoryPort.buscarPorAssinaturaMesAno(anyLong(), any(), any()))
                .thenReturn(Optional.empty());
        when(cobrancaMensalRepositoryPort.salvar(any()))
                .thenThrow(new DataIntegrityViolationException("cobrança já criada concorrentemente"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int geradas = geradorCobrancaService.gerarCobrancasDoMes(7, 2026);

        assertEquals(1, geradas);
        verify(cobrancaMensalRepositoryPort, times(2)).salvar(any());
    }
}
