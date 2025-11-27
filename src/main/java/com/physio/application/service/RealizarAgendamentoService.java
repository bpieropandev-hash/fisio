package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.RealizarAgendamentoUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealizarAgendamentoService implements RealizarAgendamentoUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ServicoRepositoryPort servicoRepositoryPort;

    @Override
    public Atendimento realizarAgendamento(Long pacienteId, Long servicoId, LocalDateTime dataHora) {
        log.info("Iniciando agendamento - Paciente: {}, Serviço: {}, Data/Hora: {}", pacienteId, servicoId, dataHora);

        // Validação: Verificar se o paciente existe e está ativo
        Paciente paciente = pacienteRepositoryPort.buscarPorId(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado ou inativo: " + pacienteId));

        // Validação: Verificar se o serviço existe e está ativo
        ServicoConfig servico = servicoRepositoryPort.buscarPorIdEAtivo(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado ou inativo: " + servicoId));

        // Criar novo Atendimento (Domain Model)
        Atendimento atendimento = Atendimento.builder()
                .paciente(paciente)
                .servicoBase(servico)
                .dataHoraInicio(dataHora)
                .dataHoraFim(servico.getNome().equals("Avaliação") ? dataHora.plusMinutes(90) : dataHora.plusMinutes(60))
                .status("AGENDADO")
                .build();

        // REGRA DE NEGÓCIO CRÍTICA: Criar snapshot financeiro
        // Copia os valores do serviço para o atendimento, garantindo histórico financeiro
        atendimento.criarSnapshotFinanceiro(servico);

        log.info("Snapshot financeiro criado - Valor: {}, % Clínica: {}, % Profissional: {}",
                atendimento.getValorCobrado(),
                atendimento.getPctClinicaSnapshot(),
                atendimento.getPctProfissionalSnapshot());

        // Salvar via porta de saída
        Atendimento atendimentoSalvo = atendimentoRepositoryPort.salvar(atendimento);

        log.info("Agendamento realizado com sucesso - ID: {}", atendimentoSalvo.getId());
        return atendimentoSalvo;
    }
}

