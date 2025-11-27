package com.physio.domain.ports.in;

import com.physio.domain.model.Atendimento;
import java.time.LocalDateTime;
import java.util.List;

public interface ListarAtendimentosUseCase {
    // Atualizado para suportar filtros opcionais
    List<Atendimento> listar(LocalDateTime inicio, LocalDateTime fim, Long pacienteId);
}