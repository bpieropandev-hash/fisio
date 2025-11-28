package com.physio.infrastructure.in.web.controller;

import com.physio.domain.ports.in.GerarCobrancasUseCase;
import com.physio.infrastructure.in.web.dto.GerarCobrancasRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/financeiro")
@Tag(name = "Financeiro", description = "Operações financeiras e geração de cobranças")
@RequiredArgsConstructor
public class FinanceiroController {

    private final GerarCobrancasUseCase gerarCobrancasUseCase;

    @Operation(
            summary = "Gerar cobranças mensais",
            description = "Gera cobranças mensais para todas as assinaturas ativas do mês/ano especificado. " +
                         "Cobranças já existentes serão ignoradas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cobranças geradas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @PostMapping("/gerar-mensalidades")
    public ResponseEntity<Map<String, Object>> gerarMensalidades(
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Mês e ano de referência")
            @RequestBody GerarCobrancasRequestDTO request) {
        log.info("Gerando cobranças mensais para {}/{}", request.getMes(), request.getAno());

        int quantidadeGerada = gerarCobrancasUseCase.gerarCobrancasDoMes(request.getMes(), request.getAno());

        Map<String, Object> response = new HashMap<>();
        response.put("mes", request.getMes());
        response.put("ano", request.getAno());
        response.put("quantidadeGerada", quantidadeGerada);
        response.put("mensagem", String.format("Foram geradas %d cobrança(s) para %d/%d", 
                                                quantidadeGerada, request.getMes(), request.getAno()));

        return ResponseEntity.ok(response);
    }
}

