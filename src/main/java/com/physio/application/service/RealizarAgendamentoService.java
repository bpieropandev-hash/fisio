package com.physio.application.service;

import com.physio.domain.model.Atendimento;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.model.StatusAtendimento;
import com.physio.domain.model.StatusPagamento;
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

        atendimentoRepositoryPort.bloquearAgenda();
        // Validações iniciais (comuns para único e recorrente)
        Paciente paciente = pacienteRepositoryPort.buscarPorId(pacienteId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Paciente não encontrado ou inativo: " + pacienteId));

        if (Boolean.FALSE.equals(paciente.getAtivo())) {
            throw new IllegalArgumentException("Paciente não está ativo: " + pacienteId);
        }

        ServicoConfig servico = servicoRepositoryPort.buscarPorIdEAtivo(servicoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Serviço não encontrado ou inativo: " + servicoId));

        // Cenário A: Agendamento Único
        if (dataFimRecorrencia == null) {
            ValidadorCapacidade.validar(atendimentoRepositoryPort, servico, paciente.getId(), dataHora, fim(servico,dataHora), null);
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

        if(datasParaAgendar.isEmpty()) throw new IllegalArgumentException("Nenhuma data corresponde aos dias selecionados");
        // Validate the entire recurrence before writing; one conflict rejects the request.
        for (LocalDate data : datasParaAgendar) {
            var inicio = data.atTime(dataHora.toLocalTime());
            ValidadorCapacidade.validar(atendimentoRepositoryPort,servico,paciente.getId(),inicio,fim(servico,inicio),null);
        }
        List<Atendimento> criados = new ArrayList<>();
        for (LocalDate data : datasParaAgendar) criados.add(criarUnico(paciente,servico,pacienteId,servicoId,data.atTime(dataHora.toLocalTime())));
        return criados;
    }
    private LocalDateTime fim(ServicoConfig servico, LocalDateTime inicio) { return inicio.plusMinutes("Avaliação".equals(servico.getNome())?90:60); }

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
                .dataHoraFim(fim(servico,dataHora))
                .status(StatusAtendimento.AGENDADO)
                .statusPagamento(StatusPagamento.PAYMENT_PENDING) // Novo atendimento sempre inicia com pagamento pendente
                .build();

        validaTemAssinatura(servico, pacienteId, servicoId, atendimento);

        // Salvar via porta de saída
        Atendimento atendimentoSalvo = atendimentoRepositoryPort.salvar(atendimento);

        log.debug("Agendamento único criado com sucesso - ID: {}", atendimentoSalvo.getId());
        return atendimentoSalvo;
    }

    private void validaTemAssinatura(ServicoConfig servico, Long pacienteId, Long servicoId, Atendimento atendimento) {
        var assinatura = assinaturaRepositoryPort.buscarAtivaPorPacienteEServico(pacienteId, servicoId)
            .filter(a -> !Boolean.FALSE.equals(a.getAtivo()))
            .filter(a -> a.getDataInicio()!=null && !atendimento.getDataHoraInicio().toLocalDate().isBefore(a.getDataInicio()))
            .filter(a -> a.getDataCancelamento()==null || atendimento.getDataHoraInicio().isBefore(a.getDataCancelamento()));
        if(assinatura.isPresent()) {
            atendimento.setAssinaturaId(assinatura.get().getId());
            atendimento.setModalidadeCobranca(com.physio.domain.model.ModalidadeCobranca.MENSALIDADE);
            atendimento.setValorCobrado(BigDecimal.ZERO);
            atendimento.setPctClinicaSnapshot(BigDecimal.ZERO);
            atendimento.setPctProfissionalSnapshot(BigDecimal.ZERO);
        } else {
            atendimento.setModalidadeCobranca(com.physio.domain.model.ModalidadeCobranca.AVULSO);
            atendimento.criarSnapshotFinanceiro(servico);
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
                if (diaInt == null || diaInt < 1 || diaInt > 7) {
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
