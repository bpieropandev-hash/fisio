package com.physio.domain.ports.in;

import com.physio.domain.model.Assinatura;

import java.math.BigDecimal;

public interface AtualizarAssinaturaUseCase {
    Assinatura atualizar(Long id, BigDecimal valorMensal, Integer diaVencimento);
}
