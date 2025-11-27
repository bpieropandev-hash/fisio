package com.physio.infrastructure.in.web.controller;

import com.physio.application.service.RelatorioPDFService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios", description = "Exportação de relatórios e prontuários em PDF")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioPDFService relatorioService;

    @Operation(summary = "Exportar Relatório Financeiro", description = "Gera um PDF contendo o detalhamento financeiro do mês (Total Clínica vs Profissional).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso",
                    content = @Content(mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary"))), // <--- Isso habilita o download no Swagger
            @ApiResponse(responseCode = "400", description = "Mês ou ano inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno na geração do PDF")
    })
    @GetMapping("/financeiro")
    public ResponseEntity<byte[]> baixarRelatorioFinanceiro(
            @Parameter(description = "Mês do relatório (1-12)", example = "11", required = true)
            @RequestParam int mes,

            @Parameter(description = "Ano do relatório", example = "2025", required = true)
            @RequestParam int ano) {

        log.info("Gerando relatório financeiro para {}/{}", mes, ano);

        byte[] pdfBytes = relatorioService.gerarRelatorioFinanceiro(mes, ano);

        return ResponseEntity.ok()
                // O header abaixo força o navegador a baixar o arquivo em vez de tentar abrir
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financeiro_" + mes + "_" + ano + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}