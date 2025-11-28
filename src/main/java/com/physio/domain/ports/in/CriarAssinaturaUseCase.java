package com.physio.domain.ports.in;

import com.physio.domain.model.Assinatura;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CriarAssinaturaUseCase {
    Assinatura criarAssinatura(Long pacienteId, Long servicoId, BigDecimal valorMensal, Integer diaVencimento, LocalDate dataInicio);
}

