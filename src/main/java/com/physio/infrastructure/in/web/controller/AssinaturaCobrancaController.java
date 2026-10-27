package com.physio.infrastructure.in.web.controller;

import com.physio.domain.ports.in.GerarCobrancaAssinaturaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assinaturas")
@RequiredArgsConstructor
public class AssinaturaCobrancaController {
    private final GerarCobrancaAssinaturaUseCase gerarCobranca;

    public record Resultado(boolean gerada) {}

    @Operation(summary = "Gerar cobrança do mês corrente para uma assinatura", description = "Idempotente: mantém a cobrança e os snapshots existentes.")
    @PostMapping("/{id}/cobranca-mes-atual")
    public Resultado gerar(@PathVariable Long id) {
        return new Resultado(gerarCobranca.gerarCobrancaMesAtual(id));
    }
}
