package com.physio.infrastructure.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtendimentoUpdateRequestDTO {

    @Schema(description = "Nova data/hora de início", example = "2025-12-01T11:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraInicio;

    @Schema(description = "Status do atendimento (AGENDADO, CONCLUIDO, CANCELADO, FALTA)", example = "CONCLUIDO")
    private String status;

    @Schema(description = "Texto de evolução / prontuário")
    private String evolucao;
}
