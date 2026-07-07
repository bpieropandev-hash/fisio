package com.physio.infrastructure.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadFotoRequestDTO(
        @NotBlank(message = "Foto é obrigatória")
        String fotoBase64
) {
}
