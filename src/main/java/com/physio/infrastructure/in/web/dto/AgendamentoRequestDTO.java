package com.physio.infrastructure.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoRequestDTO {
    @NotNull(message = "A lista de IDs de pacientes é obrigatória")
    @Schema(example = "[1, 2, 3]")
    private List<Long> pacienteIds;

    @NotNull(message = "ID do serviço é obrigatório")
    @Schema(example = "2")
    private Long servicoId;

    @NotNull(message = "Data e hora de início são obrigatórias")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data e hora de início no formato ISO (ou data única se não houver recorrência)", 
            example = "2025-12-01T10:00:00")
    private LocalDateTime dataHora;

    // Campos opcionais para recorrência
    @Schema(description = "Data final da recorrência (opcional). Se informado, cria agendamentos recorrentes até esta data", 
            example = "2025-12-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataFimRecorrencia;

    @Schema(description = "Dias da semana para repetir (opcional). Valores: 1=Segunda, 2=Terça, 3=Quarta, 4=Quinta, 5=Sexta, 6=Sábado, 7=Domingo. " +
                         "Ex: [1, 3, 5] para Segunda, Quarta e Sexta. Se não informado e dataFimRecorrencia estiver presente, " +
                         "repetirá no mesmo dia da semana da dataHora",
            example = "[1, 3, 5]")
    private List<Integer> diasSemana;
}
