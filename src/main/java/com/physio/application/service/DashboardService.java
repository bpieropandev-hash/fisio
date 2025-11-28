package com.physio.application.service;

import com.physio.infrastructure.in.web.dto.DashboardStatsDTO;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.entity.CobrancaMensalEntity;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import com.physio.infrastructure.out.persistence.repository.CobrancaMensalJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AtendimentoJpaRepository atendimentoJpaRepository;
    private final CobrancaMensalJpaRepository cobrancaMensalJpaRepository;

    /**
     * Gera as estatísticas do dashboard para o mês atual
     * 
     * @return DashboardStatsDTO com total de atendimentos, faturamento da profissional e alertas de pendência
     */
    public DashboardStatsDTO obterResumoMensal() {
        LocalDate hoje = LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();

        log.info("Gerando resumo do dashboard para {}/{}", mesAtual, anoAtual);

        // Calcular início e fim do mês
        LocalDateTime inicioMes = LocalDate.of(anoAtual, mesAtual, 1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1);

        // A. Total de Atendimentos Concluídos
        Long totalAtendimentos = atendimentoJpaRepository.countConcluidosPorMes(inicioMes, fimMes);
        log.debug("Total de atendimentos concluídos: {}", totalAtendimentos);

        // B. Faturamento da Profissional
        BigDecimal faturamentoProfissional = calcularFaturamentoProfissional(anoAtual, mesAtual, inicioMes, fimMes);
        log.debug("Faturamento da profissional: R$ {}", faturamentoProfissional);

        // C. Alertas de Inadimplência
        List<String> alertasPendencia = obterPacientesInadimplentes(anoAtual, mesAtual);
        log.debug("Pacientes inadimplentes: {}", alertasPendencia.size());

        return DashboardStatsDTO.builder()
                .totalAtendimentos(totalAtendimentos)
                .faturamentoProfissional(faturamentoProfissional)
                .alertasPendencia(alertasPendencia)
                .build();
    }

    /**
     * Calcula o faturamento líquido da profissional (valor que ela tem direito)
     * Soma: Atendimentos Avulsos + Cobranças Mensais Pagas
     */
    private BigDecimal calcularFaturamentoProfissional(int ano, int mes, LocalDateTime inicioMes, LocalDateTime fimMes) {
        BigDecimal total = BigDecimal.ZERO;

        // 1. Atendimentos Avulsos (Status CONCLUIDO e Valor > 0)
        List<AtendimentoEntity> atendimentosAvulsos = atendimentoJpaRepository
                .findAvulsosConcluidosPorMes(inicioMes, fimMes);

        for (AtendimentoEntity atendimento : atendimentosAvulsos) {
            BigDecimal valorProfissional = atendimento.getValorCobrado()
                    .multiply(atendimento.getPctProfissionalSnapshot())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            total = total.add(valorProfissional);
        }

        log.debug("Faturamento de atendimentos avulsos: R$ {}", total);

        // 2. Cobranças Mensais Pagas
        List<CobrancaMensalEntity> cobrancasPagas = cobrancaMensalJpaRepository
                .findPagasPorMes(ano, mes);

        BigDecimal totalMensalidades = BigDecimal.ZERO;
        for (CobrancaMensalEntity cobranca : cobrancasPagas) {
            BigDecimal valorProfissional = cobranca.getValor()
                    .multiply(cobranca.getPctProfissionalSnapshot())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            totalMensalidades = totalMensalidades.add(valorProfissional);
        }

        log.debug("Faturamento de mensalidades: R$ {}", totalMensalidades);

        total = total.add(totalMensalidades);
        return total;
    }

    /**
     * Busca pacientes inadimplentes (cobranças pendentes vencidas)
     * Retorna lista com nomes dos pacientes
     */
    private List<String> obterPacientesInadimplentes(int anoAtual, int mesAtual) {
        List<CobrancaMensalEntity> cobrancasPendentes = cobrancaMensalJpaRepository
                .findPendentesVencidas(anoAtual, mesAtual);

        return cobrancasPendentes.stream()
                .map(c -> c.getAssinatura().getPaciente().getNome())
                .distinct() // Remove duplicatas (mesmo paciente pode ter múltiplas pendências)
                .sorted()
                .collect(Collectors.toList());
    }
}

