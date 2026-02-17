package com.physio.infrastructure.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estatísticas do dashboard para o mês atual")
public class DashboardStatsDTO {
    
    @Schema(description = "Total de atendimentos concluídos no mês", example = "45")
    private Long totalAtendimentos;
    
    @Schema(description = "Total de atendimentos AVULSOS concluídos com pagamento pendente (PAYMENT_PENDING). " +
            "Não inclui atendimentos com assinatura, pois o pagamento é via mensalidade.", example = "5")
    private Long atendimentosPagamentoPendente;
    
    @Schema(description = "Faturamento líquido da profissional (valor que ela tem direito) - apenas atendimentos PAID", example = "12500.50")
    private BigDecimal faturamentoProfissional;
    
    @Schema(description = "Lista de pendências de pagamento com tipo (ASSINATURA ou ATENDIMENTO_AVULSO)", 
            example = "[{\"nomePaciente\": \"João Silva\", \"tipoPendencia\": \"ATENDIMENTO_AVULSO\", \"descricao\": \"Atendimento concluído em 02/02/2026 - Pagamento pendente\"}]")
    private List<PendenciaDTO> alertasPendencia;
}


