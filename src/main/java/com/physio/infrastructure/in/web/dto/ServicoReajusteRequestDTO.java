package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoReajusteRequestDTO {
    @NotNull(message = "Novo valor base é obrigatório")
    @Min(value = 0, message = "Novo valor base deve ser positivo")
    @Schema(example = "190.00")
    private BigDecimal novoValorBase;

    @Schema(description = "Atualiza valor_mensal das assinaturas ativas deste serviço", example = "true")
    @Builder.Default
    private Boolean aplicarEmAssinaturasAtivas = true;

    @Schema(description = "Não sobrescreve assinaturas com valor negociado (diferente do valor de tabela anterior)", example = "true")
    @Builder.Default
    private Boolean somenteAssinaturasNoValorAntigo = true;

    @Schema(description = "Reajusta também cobranças PENDENTES do mês corrente", example = "false")
    @Builder.Default
    private Boolean atualizarCobrancasPendentes = false;
    @jakarta.validation.Valid
    @Schema(description = "Edição opcional do catálogo, aplicada na mesma transação do reajuste")
    private ServicoCreateRequestDTO dadosServico;
}
