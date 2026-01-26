package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.model.TipoServico;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealizarAgendamentoService implements RealizarAgendamentoUseCase {

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ServicoRepositoryPort servicoRepositoryPort;
    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private static final int LIMITE_PILATES = 15;
    private static final int LIMITE_FISIOTERAPIA = 1;

    @Override
    @Transactional
    public List<Atendimento> realizarAgendamento(
            Long pacienteId,
            Long servicoId,
            LocalDateTime dataHora,
            LocalDate dataFimRecorrencia,
            List<Integer> diasSemana, Integer countPacientes) {

        log.info("Iniciando agendamento - Paciente: {}, Serviço: {}, Data/Hora: {}, Recorrente: {}",
                pacienteId, servicoId, dataHora, dataFimRecorrencia != null);

        // Validações iniciais (comuns para único e recorrente)
        Paciente paciente = pacienteRepositoryPort.buscarPorId(pacienteId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Paciente não encontrado ou inativo: " + pacienteId));

        ServicoConfig servico = servicoRepositoryPort.buscarPorIdEAtivo(servicoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Serviço não encontrado ou inativo: " + servicoId));

        // Cenário A: Agendamento Único
        if (dataFimRecorrencia == null) {
            validarQuantidadePacientes(dataHora, servico, countPacientes);
            Atendimento atendimento = criarUnico(paciente, servico, pacienteId, servicoId, dataHora);
            return List.of(atendimento);
        }

        // Cenário B: Agendamento Recorrente
        if (dataFimRecorrencia.isBefore(dataHora.toLocalDate())) {
            throw new IllegalArgumentException("dataFimRecorrência deve ser posterior ou igual à data de início");
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
                validarQuantidadePacientes(dataHoraAgendamento, servico, countPacientes);
                Atendimento atendimento = criarUnico(paciente, servico, pacienteId, servicoId, dataHoraAgendamento);
                atendimentosCriados.add(atendimento);
                sucessos++;

            } catch (IllegalArgumentException e) {
                // Captura exceções de validação (conflito de horário, etc.)
                falhas++;
                log.warn("Falha ao criar agendamento para data {}: {}", data, e.getMessage());
                // Continua a tentar criar os demais agendamentos
            } catch (Exception e) {
                falhas++;
                log.error("Erro inesperado ao criar agendamento para data {}: {}", data, e.getMessage(), e);
                // Continua a tentar criar os demais agendamentos
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

        validaTemAssinatura(servico, pacienteId, servicoId, atendimento);

        // Salvar via porta de saída
        Atendimento atendimentoSalvo = atendimentoRepositoryPort.salvar(atendimento);

        log.debug("Agendamento único criado com sucesso - ID: {}", atendimentoSalvo.getId());
        return atendimentoSalvo;
    }

    private void validaTemAssinatura(ServicoConfig servico, Long pacienteId, Long servicoId, Atendimento atendimento) {
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

    private void validarQuantidadePacientes(
            LocalDateTime dataHora,
            ServicoConfig servico,
            Integer countPacientes) {

        // Define duração
        LocalDateTime dataHoraFim = servico.getNome().equals("Avaliação")
                ? dataHora.plusMinutes(90)
                : dataHora.plusMinutes(60);

        List<Atendimento> conflitos = atendimentoRepositoryPort
                .listarConflitosPorPeriodo(dataHora, dataHoraFim);

        // Regra: não pode misturar tipos de serviço no mesmo horário
        boolean possuiTipoDiferente = conflitos.stream()
                .anyMatch(a -> a.getServicoBase().getTipo() != servico.getTipo());

        if (possuiTipoDiferente) {
            throw new IllegalArgumentException(
                    "Tipo de serviço diferente de outros agendamentos no mesmo horário."
            );
        }

        int totalNoHorario = conflitos.size() + countPacientes;

        if (servico.getTipo() == TipoServico.PILATES) {

            if (totalNoHorario > LIMITE_PILATES) {
                log.warn(
                        "Capacidade máxima de {} pacientes atingida para PILATES no horário {} - total={}",
                        LIMITE_PILATES, dataHora, totalNoHorario
                );
                throw new IllegalArgumentException(
                        "Capacidade máxima para PILATES neste horário atingida (5 pacientes)"
                );
            }

        } else if (servico.getTipo() == TipoServico.FISIOTERAPIA && totalNoHorario > LIMITE_FISIOTERAPIA) {

            log.warn(
                    "Conflito para FISIOTERAPIA no horário {} - total={}",
                    dataHora, totalNoHorario
            );
            throw new IllegalArgumentException(
                    "Já existe um agendamento neste horário!"
            );

        }
    }
}
