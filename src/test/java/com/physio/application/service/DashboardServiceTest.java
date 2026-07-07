package com.physio.application.service;

import com.physio.infrastructure.in.web.dto.DashboardStatsDTO;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.entity.CobrancaMensalEntity;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import com.physio.infrastructure.out.persistence.repository.CobrancaMensalJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AtendimentoJpaRepository atendimentoJpaRepository;

    @Mock
    private CobrancaMensalJpaRepository cobrancaMensalJpaRepository;

    private DashboardService dashboardService;

    private final LocalDate inicio = LocalDate.of(2026, 7, 1);
    private final LocalDate fim = LocalDate.of(2026, 7, 31);

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(atendimentoJpaRepository, cobrancaMensalJpaRepository);

        lenient().when(atendimentoJpaRepository.countConcluidosPorMes(any(), any())).thenReturn(0L);
        lenient().when(atendimentoJpaRepository.countConcluidosComPagamentoPendentePorMes(any(), any())).thenReturn(0L);
        lenient().when(atendimentoJpaRepository.findAvulsosConcluidosPorMes(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(atendimentoJpaRepository.findConcluidosComPagamentoPendente()).thenReturn(Collections.emptyList());
        lenient().when(cobrancaMensalJpaRepository.findPagasPorPeriodo(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
        lenient().when(cobrancaMensalJpaRepository.findPendentesVencidas(anyInt(), anyInt())).thenReturn(Collections.emptyList());
    }

    @Test
    void deveRejeitarPeriodoComInicioDepoisDoFim() {
        assertThrows(IllegalArgumentException.class,
                () -> dashboardService.obterResumo(fim, inicio));
    }

    @Test
    void deveSomarApenasAtendimentosAvulsosPagos() {
        AtendimentoEntity pago = AtendimentoEntity.builder()
                .valorCobrado(new BigDecimal("100.00"))
                .pctProfissionalSnapshot(new BigDecimal("80"))
                .statusPagamento("PAID")
                .build();
        AtendimentoEntity pendente = AtendimentoEntity.builder()
                .valorCobrado(new BigDecimal("200.00"))
                .pctProfissionalSnapshot(new BigDecimal("80"))
                .statusPagamento("PAYMENT_PENDING")
                .build();

        when(atendimentoJpaRepository.findAvulsosConcluidosPorMes(any(), any()))
                .thenReturn(List.of(pago, pendente));

        DashboardStatsDTO stats = dashboardService.obterResumo(inicio, fim);

        // Apenas o atendimento PAID entra: 100 * 80% = 80.00
        assertEquals(new BigDecimal("80.00"), stats.getFaturamentoProfissional());
    }

    @Test
    void deveSomarCobrancasMensaisPagasComSnapshotDePercentual() {
        CobrancaMensalEntity cobranca = CobrancaMensalEntity.builder()
                .valor(new BigDecimal("180.00"))
                .pctProfissionalSnapshot(new BigDecimal("70"))
                .build();

        when(cobrancaMensalJpaRepository.findPagasPorPeriodo(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(cobranca));

        DashboardStatsDTO stats = dashboardService.obterResumo(inicio, fim);

        // 180 * 70% = 126.00
        assertEquals(new BigDecimal("126.00"), stats.getFaturamentoProfissional());
    }

    @Test
    void deveSomarAvulsosECobrancasNoMesmoPeriodo() {
        AtendimentoEntity avulso = AtendimentoEntity.builder()
                .valorCobrado(new BigDecimal("100.00"))
                .pctProfissionalSnapshot(new BigDecimal("80"))
                .statusPagamento("PAID")
                .build();
        CobrancaMensalEntity cobranca = CobrancaMensalEntity.builder()
                .valor(new BigDecimal("180.00"))
                .pctProfissionalSnapshot(new BigDecimal("70"))
                .build();

        when(atendimentoJpaRepository.findAvulsosConcluidosPorMes(any(), any()))
                .thenReturn(List.of(avulso));
        when(cobrancaMensalJpaRepository.findPagasPorPeriodo(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(cobranca));

        DashboardStatsDTO stats = dashboardService.obterResumo(inicio, fim);

        // 80.00 (avulso) + 126.00 (mensalidade) = 206.00
        assertEquals(new BigDecimal("206.00"), stats.getFaturamentoProfissional());
    }
}
