package com.physio.infrastructure.in.web.dto;

import com.physio.domain.model.Recebedor;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.model.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CobrancaMensalUpdateRequestDTO {
    @NotNull(message = "Status é obrigatório")
    @Schema(example = "PAGO")
    private StatusCobranca status;

    @Schema(description = "Data de pagamento (obrigatória quando status é PAGO)", example = "2025-11-15")
    private LocalDate dataPagamento;

    @Schema(description = "Quem recebeu o pagamento (obrigatório quando status é PAGO)", example = "CLINICA")
    private Recebedor recebedor;

    @Schema(description = "Forma de pagamento", example = "PIX")
    private TipoPagamento tipoPagamento;
}

