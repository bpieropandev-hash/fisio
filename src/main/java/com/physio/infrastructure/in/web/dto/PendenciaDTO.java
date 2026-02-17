package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informação sobre pendência de pagamento de um paciente")
public class PendenciaDTO {
    
    @Schema(description = "Nome do paciente", example = "João Silva")
    private String nomePaciente;
    
    @Schema(description = "Tipo de pendência: ASSINATURA (mensalidade) ou ATENDIMENTO_AVULSO", 
            example = "ATENDIMENTO_AVULSO",
            allowableValues = {"ASSINATURA", "ATENDIMENTO_AVULSO"})
    private String tipoPendencia;
    
    @Schema(description = "Descrição da pendência", example = "Atendimento concluído em 02/02/2026 - Pagamento pendente")
    private String descricao;
}
