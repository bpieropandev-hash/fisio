package com.physio.infrastructure.in.web.controller;

import com.physio.application.service.DashboardService;
import com.physio.infrastructure.in.web.dto.DashboardStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Estatísticas e resumo do sistema")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Obter resumo do dashboard",
            description = "Retorna estatísticas do período informado (padrão: mês atual): total de atendimentos concluídos, " +
                         "atendimentos avulsos com pagamento pendente, faturamento líquido da profissional " +
                         "(apenas atendimentos PAID) e lista de pendências com tipo (ASSINATURA ou ATENDIMENTO_AVULSO). " +
                         "Parâmetros opcionais inicio/fim (yyyy-MM-dd) delimitam o período."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso")
    })
    @GetMapping("/resumo")
    public ResponseEntity<DashboardStatsDTO> obterResumo(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        log.info("Recebendo requisição de resumo do dashboard (inicio={}, fim={})", inicio, fim);

        DashboardStatsDTO resumo = dashboardService.obterResumo(inicio, fim);

        log.info("Resumo do dashboard gerado - Atendimentos: {}, Pagamento Pendente: {}, Faturamento: R$ {}, Pendências: {}",
                resumo.getTotalAtendimentos(),
                resumo.getAtendimentosPagamentoPendente(),
                resumo.getFaturamentoProfissional(),
                resumo.getAlertasPendencia().size());

        return ResponseEntity.ok(resumo);
    }
}


