package com.physio.infrastructure.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ConcluirAtendimentosLoteRequestDTO(
        @NotEmpty(message = "Informe ao menos um atendimento")
        @Valid
        List<ConcluirAtendimentoLoteItemDTO> atendimentos
) {
}
