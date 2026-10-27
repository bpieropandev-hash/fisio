package com.physio.infrastructure.in.web.dto;

import jakarta.validation.constraints.*;

public record ServicoCapacidadeRequestDTO(@NotNull @Min(1) @Max(100) Integer capacidade) {}
