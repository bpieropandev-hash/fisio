package com.physio.domain.ports.in;

import com.physio.domain.model.ReajusteResultado;
import com.physio.domain.model.ServicoConfig;

import java.math.BigDecimal;

public interface ReajustarServicoUseCase {
    ReajusteResultado reajustar(
            Long servicoId,
            BigDecimal novoValorBase,
            boolean aplicarEmAssinaturasAtivas,
            boolean somenteAssinaturasNoValorAntigo,
            boolean atualizarCobrancasPendentes
    );
    ReajusteResultado reajustar(Long servicoId, BigDecimal novoValorBase,
            boolean aplicarEmAssinaturasAtivas, boolean somenteAssinaturasNoValorAntigo,
            boolean atualizarCobrancasPendentes, ServicoConfig dadosServico);
}
