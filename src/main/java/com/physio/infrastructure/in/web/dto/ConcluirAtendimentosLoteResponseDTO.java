package com.physio.infrastructure.in.web.dto;

import java.util.List;

public record ConcluirAtendimentosLoteResponseDTO(
        int concluidos,
        List<FalhaLoteDTO> falhas
) {
}
