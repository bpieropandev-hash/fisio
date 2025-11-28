package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.RealizarAgendamentoUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealizarAgendamentoService implements RealizarAgendamentoUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ServicoRepositoryPort servicoRepositoryPort;
    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Override
    @Transactional
    public List<Atendimento> realizarAgendamento(
            Long pacienteId,
            Long servicoId,
            LocalDateTime dataHora,
            LocalDate dataFimRecorrencia,
            List<Integer> diasSemana) {

        log.info("Iniciando agendamento - Paciente: {}, Serviço: {}, Data/Hora: {}, Recorrente: {}",
                pacienteId, servicoId, dataHora, dataFimRecorrencia != null);

        // Validações iniciais (comuns para único e recorrente)
        Paciente paciente = pacienteRepositoryPort.buscarPorId(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado ou inativo: " + pacienteId));

        ServicoConfig servico = servicoRepositoryPort.buscarPorIdEAtivo(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado ou inativo: " + servicoId));

        // Cenário A: Agendamento Único
        if (dataFimRecorrencia == null) {
            Atendimento atendimento = criarUnico(paciente, servico, pacienteId, servicoId, dataHora);
            return List.of(atendimento);
        }

        // Cenário B: Agendamento Recorrente
        if (dataFimRecorrencia.isBefore(dataHora.toLocalDate())) {
            throw new IllegalArgumentException("dataFimRecorrencia deve ser posterior ou igual à data de início");
        }

        // Determinar dias da semana para repetir
        Set<DayOfWeek> diasParaRepetir = determinarDiasSemana(dataHora, diasSemana);

        // Gerar lista de datas para agendamento
        List<LocalDate> datasParaAgendar = gerarDatasRecorrencia(
                dataHora.toLocalDate(),
                dataFimRecorrencia,
                diasParaRepetir
        );

        log.info("Serão criados {} agendamentos recorrentes", datasParaAgendar.size());

        // Criar agendamentos para cada data
        List<Atendimento> atendimentosCriados = new ArrayList<>();
        int sucessos = 0;
        int falhas = 0;

        for (LocalDate data : datasParaAgendar) {
            try {
                // Manter o mesmo horário da data inicial
                LocalDateTime dataHoraAgendamento = data.atTime(dataHora.toLocalTime());

                Atendimento atendimento = criarUnico(paciente, servico, pacienteId, servicoId, dataHoraAgendamento);
                atendimentosCriados.add(atendimento);
                sucessos++;

            } catch (IllegalArgumentException e) {
                // Captura exceções de validação (conflito de horário, etc.)
                falhas++;
                log.warn("Falha ao criar agendamento para data {}: {}", data, e.getMessage());
                // Continua tentando criar os demais agendamentos
            } catch (Exception e) {
                falhas++;
                log.error("Erro inesperado ao criar agendamento para data {}: {}", data, e.getMessage(), e);
                // Continua tentando criar os demais agendamentos
            }
        }

        log.info("Agendamento recorrente concluído - Sucessos: {}, Falhas: {}", sucessos, falhas);

        if (atendimentosCriados.isEmpty()) {
            throw new IllegalStateException("Nenhum agendamento foi criado. Verifique os conflitos de horário.");
        }

        return atendimentosCriados;
    }

    /**
     * Método privado que extrai a lógica de criação de um único agendamento
     * Contém toda a validação, verificação de conflitos e snapshot financeiro
     */
    private Atendimento criarUnico(
            Paciente paciente,
            ServicoConfig servico,
            Long pacienteId,
            Long servicoId,
            LocalDateTime dataHora) {
        log.debug("Criando agendamento único - Data/Hora: {}", dataHora);

        // Criar novo Atendimento (Domain Model)
        Atendimento atendimento = Atendimento.builder()
                .paciente(paciente)
                .servicoBase(servico)
                .dataHoraInicio(dataHora)
                .dataHoraFim(servico.getNome().equals("Avaliação") ? dataHora.plusMinutes(90) : dataHora.plusMinutes(60))
                .status("AGENDADO")
                .build();

        // Verificar conflitos de horário
        List<Atendimento> conflitos = atendimentoRepositoryPort.listarPorPeriodo(
                atendimento.getDataHoraInicio(), 
                atendimento.getDataHoraFim()
        );

        // Filtrar conflitos reais: Mantemos somente os agendamentos que realmente impedem o novo.
        // Regra: se o agendamento existente e o novo agendamento forem do mesmo serviço AND ambos os pacientes
        // possuem assinatura ativa para esse serviço (assinatura mensal), então NÃO é considerado conflito
        // (ex: aulas de pilates mensais podem ter mais de um aluno no mesmo horário).
        List<Atendimento> conflitosReais = conflitos.stream()
                .filter(existente -> {
                    // Se o existente for null por algum motivo, considera como conflito
                    if (existente == null || existente.getServicoBase() == null) return true;

                    Integer existenteServicoId = existente.getServicoBase().getId();
                    Long existenteServicoIdLong = existenteServicoId == null ? null : existenteServicoId.longValue();

                    // Se o serviço for diferente, continua sendo conflito
                    if (!Objects.equals(existenteServicoIdLong, servicoId)) return true;

                    // Ambos são do mesmo serviço - verifica assinaturas ativas
                    Integer existentePacienteIdInt = existente.getPaciente() == null ? null : existente.getPaciente().getId();
                    Long existentePacienteId = existentePacienteIdInt == null ? null : existentePacienteIdInt.longValue();

                    boolean existenteTemAssinatura = existentePacienteId != null && assinaturaRepositoryPort
                            .buscarAtivaPorPacienteEServico(existentePacienteId, servicoId)
                            .isPresent();

                    boolean novoPacienteTemAssinatura = assinaturaRepositoryPort
                            .buscarAtivaPorPacienteEServico(pacienteId, servicoId)
                            .isPresent();

                    // Se ambos tiverem assinatura ativa para esse serviço, então NÃO é conflito (permite múltiplos alunos)
                    if (existenteTemAssinatura && novoPacienteTemAssinatura) {
                        log.debug("Conflito permitido entre pacientes assinantes para o serviço {}: existentePaciente={}, novoPaciente={}",
                                servicoId, existentePacienteId, pacienteId);
                        return false; // filtra-o (não é conflito real)
                    }

                    // Em qualquer outro caso, é conflito
                    return true;
                })
                .toList();

        if (!conflitosReais.isEmpty()) {
            log.warn("Conflitos reais encontrados no período {} - {}: {}",
                    atendimento.getDataHoraInicio(), atendimento.getDataHoraFim(), conflitosReais.size());
            throw new IllegalArgumentException("Já existe um agendamento neste horário!");
        }

        // REGRA DE NEGÓCIO: Verificar se o paciente tem assinatura ativa para este serviço
        boolean temAssinaturaAtiva = assinaturaRepositoryPort
                .buscarAtivaPorPacienteEServico(pacienteId, servicoId)
                .isPresent();

        if (temAssinaturaAtiva) {
            // Se tem assinatura ativa, o atendimento não deve ser cobrado (valor = 0)
            // A cobrança será feita via mensalidade
            atendimento.setValorCobrado(BigDecimal.ZERO);
            atendimento.setPctClinicaSnapshot(BigDecimal.ZERO);
            atendimento.setPctProfissionalSnapshot(BigDecimal.ZERO);

            log.debug("Paciente possui assinatura ativa para este serviço. Atendimento será gratuito (cobrança via mensalidade)");
        } else {
            // REGRA DE NEGÓCIO CRÍTICA: Criar snapshot financeiro
            // Copia os valores do serviço para o atendimento, garantindo histórico financeiro
            atendimento.criarSnapshotFinanceiro(servico);

            log.debug("Snapshot financeiro criado - Valor: {}, % Clínica: {}, % Profissional: {}",
                    atendimento.getValorCobrado(),
                    atendimento.getPctClinicaSnapshot(),
                    atendimento.getPctProfissionalSnapshot());
        }

        // Salvar via porta de saída
        Atendimento atendimentoSalvo = atendimentoRepositoryPort.salvar(atendimento);

        log.debug("Agendamento único criado com sucesso - ID: {}", atendimentoSalvo.getId());
        return atendimentoSalvo;
    }

    /**
     * Determina os dias da semana para repetir
     * Se diasSemana for null ou vazio, usa o dia da semana da dataHoraInicio
     */
    private Set<DayOfWeek> determinarDiasSemana(LocalDateTime dataHoraInicio, List<Integer> diasSemana) {
        Set<DayOfWeek> dias = new HashSet<>();

        if (diasSemana == null || diasSemana.isEmpty()) {
            // Se não especificado, usa o dia da semana da data inicial
            dias.add(dataHoraInicio.getDayOfWeek());
            log.debug("Nenhum dia da semana especificado. Usando: {}", dataHoraInicio.getDayOfWeek());
        } else {
            // Converte os inteiros para DayOfWeek
            for (Integer diaInt : diasSemana) {
                if (diaInt < 1 || diaInt > 7) {
                    throw new IllegalArgumentException("Dia da semana inválido: " + diaInt + ". Deve estar entre 1 (Segunda) e 7 (Domingo)");
                }
                // DayOfWeek: MONDAY=1, TUESDAY=2, ..., SUNDAY=7
                dias.add(DayOfWeek.of(diaInt));
            }
            log.debug("Dias da semana especificados: {}", dias);
        }

        return dias;
    }

    /**
     * Gera lista de datas para agendamento baseado nos dias da semana
     */
    private List<LocalDate> gerarDatasRecorrencia(
            LocalDate dataInicio,
            LocalDate dataFim,
            Set<DayOfWeek> diasSemana) {

        List<LocalDate> datas = new ArrayList<>();
        LocalDate dataAtual = dataInicio;

        while (!dataAtual.isAfter(dataFim)) {
            if (diasSemana.contains(dataAtual.getDayOfWeek())) {
                datas.add(dataAtual);
            }
            dataAtual = dataAtual.plusDays(1);
        }

        return datas;
    }
}
