package com.physio.infrastructure.in.web.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record EstornoCobrancaRequestDTO(@NotBlank @Size(max=1000) String motivo) {}
