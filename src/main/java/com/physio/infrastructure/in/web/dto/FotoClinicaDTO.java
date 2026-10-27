package com.physio.infrastructure.in.web.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record FotoClinicaDTO(Long id, Integer pacienteId, LocalDate dataCaptura, String regiao,
                             String observacao, LocalDateTime criadoEm, String autor) {}
