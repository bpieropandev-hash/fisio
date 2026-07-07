package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UsuarioMeResponseDTO(
        Integer id,
        String login,
        String email,
        String nome,
        String telefone,
        @Schema(description = "Foto do usuário em base64 (data URI), se cadastrada")
        String fotoBase64,
        String role,

        @Schema(description = "Cor de acento do tema, hex #rrggbb. Null = default do sistema", example = "#c96f4a")
        String corPrimaria,

        @Schema(description = "Par de fonte do tema", allowableValues = {"acolhedor", "classico", "moderno", "editorial", "direto"})
        String fonteTema,

        @Schema(description = "Tamanho de fonte do sistema", allowableValues = {"pequeno", "medio", "grande"})
        String tamanhoFonte
) {
}
