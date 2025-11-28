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
    
    @Schema(description = "Faturamento líquido da profissional (valor que ela tem direito)", example = "12500.50")
    private BigDecimal faturamentoProfissional;
    
    @Schema(description = "Lista de nomes dos pacientes inadimplentes (cobranças pendentes)", 
            example = "[\"João Silva\", \"Maria Santos\"]")
    private List<String> alertasPendencia;
}


