package com.physio.domain.ports.in;

import com.physio.domain.model.Atendimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface RealizarAgendamentoUseCase {
    /**
     * Realiza agendamento único ou recorrente
     * 
     * @param pacienteId ID do paciente
     * @param servicoId ID do serviço
     * @param dataHora Data e hora do agendamento (ou data inicial se recorrente)
     * @param dataFimRecorrencia Data final da recorrência (null para agendamento único)
     * @param diasSemana Lista de dias da semana para repetir (1=Segunda, 2=Terça, ..., 7=Domingo). 
     *                   Se null e dataFimRecorrencia estiver presente, usa o dia da semana da dataHora
     * @return Lista de atendimentos criados (sempre retorna lista, mesmo para agendamento único)
     */
    List<Atendimento> realizarAgendamento(
            Long pacienteId,
            Long servicoId,
            LocalDateTime dataHora,
            LocalDate dataFimRecorrencia,
            List<Integer> diasSemana,
            Integer quantidade
    );
}

