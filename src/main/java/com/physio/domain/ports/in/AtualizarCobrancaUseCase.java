package com.physio.domain.ports.in;

import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.Recebedor;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.model.TipoPagamento;

import java.time.LocalDate;

public interface AtualizarCobrancaUseCase {
    CobrancaMensal atualizarCobranca(Long cobrancaId, StatusCobranca status, LocalDate dataPagamento, Recebedor recebedor, TipoPagamento tipoPagamento);
}

