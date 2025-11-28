package com.physio.infrastructure.in.web.controller;

import com.physio.application.service.DashboardService;
import com.physio.infrastructure.in.web.dto.DashboardStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Estatísticas e resumo do sistema")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Obter resumo do dashboard",
            description = "Retorna estatísticas do mês atual: total de atendimentos concluídos, " +
                         "faturamento líquido da profissional e lista de pacientes inadimplentes"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso")
    })
    @GetMapping("/resumo")
    public ResponseEntity<DashboardStatsDTO> obterResumo() {
        log.info("Recebendo requisição de resumo do dashboard");

        DashboardStatsDTO resumo = dashboardService.obterResumoMensal();

        log.info("Resumo do dashboard gerado - Atendimentos: {}, Faturamento: R$ {}, Pendências: {}",
                resumo.getTotalAtendimentos(),
                resumo.getFaturamentoProfissional(),
                resumo.getAlertasPendencia().size());

        return ResponseEntity.ok(resumo);
    }
}


