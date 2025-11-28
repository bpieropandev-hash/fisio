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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios", description = "Exportação de relatórios e prontuários em PDF")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioPDFService relatorioService;

    @Operation(summary = "Relatório de Acerto Financeiro",
            description = "Gera PDF com cálculo de repasse considerando quem recebeu o pagamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @GetMapping("/acerto-financeiro")
    public ResponseEntity<byte[]> baixarRelatorioAcerto(
            @RequestParam @Parameter(description = "Data/hora inicial (formato: yyyy-MM-dd HH:mm:ss.SSS)", example = "2025-11-01 00:00:00.000") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS") LocalDateTime inicio,
            @RequestParam @Parameter(description = "Data/hora final (formato: yyyy-MM-dd HH:mm:ss.SSS)", example = "2025-11-30 23:59:59.999") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS") LocalDateTime fim,
            @RequestParam @Parameter(description = "IDs dos serviços a filtrar", example = "1,2,3") List<Integer> servicoIds) {

        byte[] pdfBytes = relatorioService.gerarRelatorioPersonalizado(inicio, fim, servicoIds);

        String fileName = "acerto_" + inicio.toLocalDate() + "_a_" + fim.toLocalDate() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}