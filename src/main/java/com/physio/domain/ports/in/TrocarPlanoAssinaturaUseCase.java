package com.physio.domain.ports.in;

import com.physio.domain.model.TrocaPlanoResultado;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TrocarPlanoAssinaturaUseCase {
    TrocaPlanoResultado trocarPlano(Long assinaturaId, Long novoServicoId, BigDecimal novoValorMensal,
                                     Integer diaVencimento, LocalDate dataInicio);
}
