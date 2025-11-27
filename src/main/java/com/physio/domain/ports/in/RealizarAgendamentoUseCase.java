package com.physio.domain.ports.in;

import com.physio.domain.model.Atendimento;

import java.time.LocalDateTime;

public interface RealizarAgendamentoUseCase {
    Atendimento realizarAgendamento(Long pacienteId, Long servicoId, LocalDateTime dataHora);
}

