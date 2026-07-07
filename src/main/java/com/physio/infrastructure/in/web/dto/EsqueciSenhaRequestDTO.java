package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequestDTO(
        @NotBlank(message = "Informe o login ou email")
        @Schema(description = "Login ou email do usuário", example = "admin")
        String loginOuEmail
) {
}
