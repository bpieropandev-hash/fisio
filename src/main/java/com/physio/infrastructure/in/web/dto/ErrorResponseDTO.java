package com.physio.infrastructure.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Contrato padrão de erro da API: { "status": 400, "mensagem": "...", "erros": ["campo: detalhe"] }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta padrão de erro da API")
public record ErrorResponseDTO(
        @Schema(example = "400") int status,
        @Schema(example = "Erro de validação") String mensagem,
        @Schema(description = "Detalhes por campo, quando houver") List<String> erros
) {
    public ErrorResponseDTO(int status, String mensagem) {
        this(status, mensagem, null);
    }
}
