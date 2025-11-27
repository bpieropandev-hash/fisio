package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        @Schema(description = "Token JWT para autenticação nas requisições protegidas")
        String token
) {
}