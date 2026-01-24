package com.physio.domain.ports.in;

import com.physio.domain.model.Assinatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CriarAssinaturaUseCase {
    List<Assinatura> criarAssinatura(List<Long> pacienteIds, Long servicoId, BigDecimal valorMensal, Integer diaVencimento, LocalDate dataInicio);
}
