package com.physio.infrastructure.in.web.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record ReciboRequestDTO(@Positive Integer atendimentoId, @Positive Integer cobrancaId,
 @NotNull @PastOrPresent LocalDate dataPagamento, @NotBlank @Size(max=255) String pagadorNome,
 @NotBlank @Size(max=30) String pagadorDocumento) {}
