package com.physio.infrastructure.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtendimentoResponseDTO {
    private Integer id;
    private Integer pacienteId;
    private Integer servicoBaseId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2025-12-01T10:00:00")
    private LocalDateTime dataHoraInicio;
    
    private BigDecimal valorCobrado;
    private BigDecimal pctClinicaSnapshot;
    private BigDecimal pctProfissionalSnapshot;
    private String status;
}
