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
     * Gera as estatísticas do dashboard para o mês atual (comportamento padrão).
     *
     * @return DashboardStatsDTO com total de atendimentos, faturamento da profissional e alertas de pendência
     */
    public DashboardStatsDTO obterResumoMensal() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        return obterResumo(inicioMes, fimMes);
    }

    /**
     * Gera as estatísticas do dashboard para um período arbitrário [inicio, fim] (ambos inclusivos).
     * Parâmetros nulos assumem o mês atual (início = dia 1, fim = último dia).
     *
     * @param inicio primeiro dia do período (inclusivo); null = dia 1 do mês atual
     * @param fim    último dia do período (inclusivo); null = último dia do mês atual
     */
    public DashboardStatsDTO obterResumo(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now();
        if (inicio == null) inicio = hoje.withDayOfMonth(1);
        if (fim == null) fim = hoje.withDayOfMonth(hoje.lengthOfMonth());
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim");
        }

        log.info("Gerando resumo do dashboard para o período {} a {}", inicio, fim);

        // Atendimentos são filtrados por dataHoraInicio no intervalo [inicioDt, fimDt) — fim exclusivo no dia seguinte
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime fimDt = fim.plusDays(1).atStartOfDay();

        // A. Total de Atendimentos Concluídos
        Long totalAtendimentos = atendimentoJpaRepository.countConcluidosPorMes(inicioDt, fimDt);
        log.debug("Total de atendimentos concluídos: {}", totalAtendimentos);

        // B. Atendimentos com Pagamento Pendente (apenas avulsos)
        Long atendimentosPagamentoPendente = atendimentoJpaRepository
                .countConcluidosComPagamentoPendentePorMes(inicioDt, fimDt);
        log.debug("Atendimentos com pagamento pendente: {}", atendimentosPagamentoPendente);

        // C. Faturamento da Profissional
        BigDecimal faturamentoProfissional = calcularFaturamentoProfissional(inicio, fim, inicioDt, fimDt);
        log.debug("Faturamento da profissional: R$ {}", faturamentoProfissional);

        // D. Alertas de Inadimplência
        List<PendenciaDTO> alertasPendencia = obterPacientesInadimplentes(inicio, fim, inicioDt, fimDt);
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
     * Nota: Apenas atendimentos com statusPagamento = PAID são considerados no faturamento.
     * Cobranças mensais são casadas pelo mês/ano de referência que cai dentro do período.
     */
    private BigDecimal calcularFaturamentoProfissional(LocalDate inicio, LocalDate fim,
                                                       LocalDateTime inicioDt, LocalDateTime fimDt) {
        // P2.3: soma em banco (SUM) em vez de carregar as entidades pra somar em Java.

        // 1. Atendimentos Avulsos (Status CONCLUIDO, Valor > 0 e StatusPagamento = PAID)
        BigDecimal totalAvulsos = atendimentoJpaRepository
                .somarFaturamentoProfissionalAvulsos(inicioDt, fimDt);
        log.debug("Faturamento de atendimentos avulsos pagos: R$ {}", totalAvulsos);

        // 2. Cobranças Mensais Pagas cujo mês/ano de referência está dentro do período
        BigDecimal totalMensalidades = cobrancaMensalJpaRepository
                .somarValorProfissionalPagoPorPeriodo(inicio.getYear(), inicio.getMonthValue(),
                        fim.getYear(), fim.getMonthValue());
        log.debug("Faturamento de mensalidades: R$ {}", totalMensalidades);

        return totalAvulsos.add(totalMensalidades);
    }

    /**
     * Busca pacientes inadimplentes dentro do período [inicio, fim]:
     * 1. Cobranças mensais pendentes vencidas até o fim do período - tipo ASSINATURA
     * 2. Atendimentos avulsos concluídos com pagamento pendente cuja data cai no período - tipo ATENDIMENTO_AVULSO
     * Retorna lista consolidada com informações detalhadas incluindo tipo de pendência
     */
    private List<PendenciaDTO> obterPacientesInadimplentes(LocalDate inicio, LocalDate fim,
                                                           LocalDateTime inicioDt, LocalDateTime fimDt) {
        List<PendenciaDTO> pendencias = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 1. Pacientes com cobranças mensais pendentes vencidas até o fim do período (ASSINATURA)
        List<CobrancaMensalEntity> cobrancasPendentes = cobrancaMensalJpaRepository
                .findPendentesVencidas(fim.getYear(), fim.getMonthValue());

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

        // 2. Pacientes com atendimentos avulsos concluídos mas não pagos, dentro do período (ATENDIMENTO_AVULSO)
        List<AtendimentoEntity> atendimentosPendentes = atendimentoJpaRepository
                .findConcluidosComPagamentoPendente();

        for (AtendimentoEntity atendimento : atendimentosPendentes) {
            LocalDateTime dataAtendimento = atendimento.getDataHoraInicio();
            // Filtra pelo período selecionado (a query não tem recorte de data)
            if (dataAtendimento.isBefore(inicioDt) || !dataAtendimento.isBefore(fimDt)) {
                continue;
            }
            String nomePaciente = atendimento.getPaciente().getNome();
            String dataFormatada = dataAtendimento.format(formatter);
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

