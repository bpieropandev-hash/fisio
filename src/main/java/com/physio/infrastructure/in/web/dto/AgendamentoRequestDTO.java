package com.physio.infrastructure.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoRequestDTO {
    @NotNull(message = "ID do paciente é obrigatório")
    @Schema(example = "1")
    private Long pacienteId;

    @NotNull(message = "ID do serviço é obrigatório")
    @Schema(example = "2")
    private Long servicoId;

    @NotNull(message = "Data e hora de início são obrigatórias")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data e hora de início no formato ISO", example = "2025-12-01T10:00:00")
    private LocalDateTime dataHora;
}
