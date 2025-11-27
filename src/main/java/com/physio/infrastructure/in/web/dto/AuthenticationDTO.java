package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(
        @NotBlank(message = "O login é obrigatório")
        @Schema(description = "Login do usuário", example = "admin")
        String login,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(description = "Senha do usuário", example = "123456")
        String senha
) {
}