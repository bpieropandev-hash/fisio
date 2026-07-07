package com.physio.infrastructure.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AtualizarUsuarioRequestDTO(
        @NotBlank(message = "Login é obrigatório")
        String login,

        String nome,
        String telefone,

        @Email(message = "Email inválido")
        String email,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Cor deve estar no formato hex #rrggbb")
        String corPrimaria,

        @Pattern(regexp = "^(acolhedor|classico|moderno|editorial|direto)$", message = "Par de fonte inválido")
        String fonteTema,

        @Pattern(regexp = "^(pequeno|medio|grande)$", message = "Tamanho de fonte inválido")
        String tamanhoFonte
) {
}
