package com.physio.application.service;

import com.physio.infrastructure.in.web.dto.DashboardStatsDTO;
import com.physio.infrastructure.in.web.dto.PendenciaDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

        // B. Atendimentos com Pagamento Pendente (apenas avulsos)
        Long atendimentosPagamentoPendente = atendimentoJpaRepository
                .countConcluidosComPagamentoPendentePorMes(inicioMes, fimMes);
        log.debug("Atendimentos com pagamento pendente: {}", atendimentosPagamentoPendente);

        // C. Faturamento da Profissional
        BigDecimal faturamentoProfissional = calcularFaturamentoProfissional(anoAtual, mesAtual, inicioMes, fimMes);
        log.debug("Faturamento da profissional: R$ {}", faturamentoProfissional);

        // D. Alertas de Inadimplência
        List<PendenciaDTO> alertasPendencia = obterPacientesInadimplentes(anoAtual, mesAtual);
        log.debug("Pacientes inadimplentes: {}", alertasPendencia.size());

        return DashboardStatsDTO.builder()
                .totalAtendimentos(totalAtendimentos)
                .atendimentosPagamentoPendente(atendimentosPagamentoPendente)
                .faturamentoProfissional(faturamentoProfissional)
                .alertasPendencia(alertasPendencia)
                .build();
    }

    /**
     * Calcula o faturamento líquido da profissional (valor que ela tem direito)
     * Soma: Atendimentos Avulsos Pagos (PAID) + Cobranças Mensais Pagas
     * Nota: Apenas atendimentos com statusPagamento = PAID são considerados no faturamento
     */
    private BigDecimal calcularFaturamentoProfissional(int ano, int mes, LocalDateTime inicioMes, LocalDateTime fimMes) {
        BigDecimal total = BigDecimal.ZERO;

        // 1. Atendimentos Avulsos (Status CONCLUIDO, Valor > 0 e StatusPagamento = PAID)
        List<AtendimentoEntity> atendimentosAvulsos = atendimentoJpaRepository
                .findAvulsosConcluidosPorMes(inicioMes, fimMes);

        for (AtendimentoEntity atendimento : atendimentosAvulsos) {
            // Considerar apenas atendimentos pagos (PAID) no faturamento
            if (atendimento.getStatusPagamento() != null && 
                "PAID".equals(atendimento.getStatusPagamento())) {
                BigDecimal valorProfissional = atendimento.getValorCobrado()
                        .multiply(atendimento.getPctProfissionalSnapshot())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                total = total.add(valorProfissional);
            }
        }

        log.debug("Faturamento de atendimentos avulsos pagos: R$ {}", total);

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
     * Busca pacientes inadimplentes:
     * 1. Cobranças mensais pendentes vencidas (mês atual ou anteriores) - tipo ASSINATURA
     * 2. Atendimentos avulsos concluídos com pagamento pendente - tipo ATENDIMENTO_AVULSO
     * Retorna lista consolidada com informações detalhadas incluindo tipo de pendência
     */
    private List<PendenciaDTO> obterPacientesInadimplentes(int anoAtual, int mesAtual) {
        List<PendenciaDTO> pendencias = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 1. Pacientes com cobranças mensais pendentes vencidas (ASSINATURA)
        List<CobrancaMensalEntity> cobrancasPendentes = cobrancaMensalJpaRepository
                .findPendentesVencidas(anoAtual, mesAtual);

        for (CobrancaMensalEntity cobranca : cobrancasPendentes) {
            String nomePaciente = cobranca.getAssinatura().getPaciente().getNome();
            String descricao = String.format("Mensalidade %02d/%04d - Valor: R$ %.2f",
                    cobranca.getMesReferencia(),
                    cobranca.getAnoReferencia(),
                    cobranca.getValor());

            pendencias.add(PendenciaDTO.builder()
                    .nomePaciente(nomePaciente)
                    .tipoPendencia("ASSINATURA")
                    .descricao(descricao)
                    .build());
        }

        // 2. Pacientes com atendimentos avulsos concluídos mas não pagos (ATENDIMENTO_AVULSO)
        List<AtendimentoEntity> atendimentosPendentes = atendimentoJpaRepository
                .findConcluidosComPagamentoPendente();

        for (AtendimentoEntity atendimento : atendimentosPendentes) {
            String nomePaciente = atendimento.getPaciente().getNome();
            String dataFormatada = atendimento.getDataHoraInicio().format(formatter);
            String descricao = String.format("Atendimento concluído em %s - Valor: R$ %.2f - Pagamento pendente",
                    dataFormatada,
                    atendimento.getValorCobrado());

            pendencias.add(PendenciaDTO.builder()
                    .nomePaciente(nomePaciente)
                    .tipoPendencia("ATENDIMENTO_AVULSO")
                    .descricao(descricao)
                    .build());
        }

        // Ordenar por nome do paciente
        return pendencias.stream()
                .sorted((p1, p2) -> p1.getNomePaciente().compareToIgnoreCase(p2.getNomePaciente()))
                .collect(Collectors.toList());
    }
}

