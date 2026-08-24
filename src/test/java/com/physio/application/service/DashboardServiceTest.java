package com.physio.application.service;

import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.in.web.dto.DashboardStatsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AtendimentoRepositoryPort atendimentoRepositoryPort;

    @Mock
    private CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    private DashboardService dashboardService;

    private final LocalDate inicio = LocalDate.of(2026, 7, 1);
    private final LocalDate fim = LocalDate.of(2026, 7, 31);

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(atendimentoRepositoryPort, cobrancaMensalRepositoryPort);

        lenient().when(atendimentoRepositoryPort.contarConcluidosPorMes(any(), any())).thenReturn(0L);
        lenient().when(atendimentoRepositoryPort.contarConcluidosComPagamentoPendentePorMes(any(), any())).thenReturn(0L);
        lenient().when(atendimentoRepositoryPort.somarFaturamentoProfissionalAvulsos(any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(atendimentoRepositoryPort.listarConcluidosComPagamentoPendente()).thenReturn(Collections.emptyList());
        lenient().when(cobrancaMensalRepositoryPort.somarValorProfissionalPagoPorPeriodo(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(BigDecimal.ZERO);
        lenient().when(cobrancaMensalRepositoryPort.buscarPendentesVencidas(anyInt(), anyInt())).thenReturn(Collections.emptyList());
    }

    @Test
    void deveRejeitarPeriodoComInicioDepoisDoFim() {
        assertThrows(IllegalArgumentException.class,
                () -> dashboardService.obterResumo(fim, inicio));
    }

    @Test
    void deveSomarFaturamentoDeAtendimentosAvulsos() {
        // A soma de PAID vs PAYMENT_PENDING agora é filtrada em SQL (P2.3) — o unit test
        // verifica só a soma vinda das duas fontes; a filtragem correta em si é coberta
        // pelo teste de integração (P1.2.3, AtendimentoJpaRepositoryIT).
        when(atendimentoRepositoryPort.somarFaturamentoProfissionalAvulsos(any(), any()))
                .thenReturn(new BigDecimal("80.00"));

        DashboardStatsDTO stats = dashboardService.obterResumo(inicio, fim);

        assertEquals(new BigDecimal("80.00"), stats.getFaturamentoProfissional());
    }

    @Test
    void deveSomarFaturamentoDeCobrancasMensais() {
        when(cobrancaMensalRepositoryPort.somarValorProfissionalPagoPorPeriodo(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("126.00"));

        DashboardStatsDTO stats = dashboardService.obterResumo(inicio, fim);

        assertEquals(new BigDecimal("126.00"), stats.getFaturamentoProfissional());
    }

    @Test
    void deveSomarAvulsosECobrancasNoMesmoPeriodo() {
        when(atendimentoRepositoryPort.somarFaturamentoProfissionalAvulsos(any(), any()))
                .thenReturn(new BigDecimal("80.00"));
        when(cobrancaMensalRepositoryPort.somarValorProfissionalPagoPorPeriodo(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("126.00"));

        DashboardStatsDTO stats = dashboardService.obterResumo(inicio, fim);

        // 80.00 (avulso) + 126.00 (mensalidade) = 206.00
        assertEquals(new BigDecimal("206.00"), stats.getFaturamentoProfissional());
    }
}
