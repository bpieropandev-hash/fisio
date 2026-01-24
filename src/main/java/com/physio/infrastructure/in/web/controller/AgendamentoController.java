package com.physio.infrastructure.in.web.controller;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.in.RealizarAgendamentoUseCase;
import com.physio.infrastructure.in.web.dto.AgendamentoRequestDTO;
import com.physio.infrastructure.in.web.dto.AtendimentoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agendamentos")
@Tag(name = "Agendamentos", description = "Operações para agendar, visualizar e gerenciar atendimentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final RealizarAgendamentoUseCase realizarAgendamentoUseCase;
    private final com.physio.domain.ports.in.BuscarAtendimentoUseCase buscarAtendimentoUseCase;
    private final com.physio.domain.ports.in.ListarAtendimentosUseCase listarAtendimentosUseCase;
    private final com.physio.domain.ports.in.AtualizarAtendimentoUseCase atualizarAtendimentoUseCase;
    private final com.physio.domain.ports.in.DeletarAtendimentoUseCase deletarAtendimentoUseCase;

    @Operation(
            summary = "Realizar agendamento", 
            description = "Cria um ou múltiplos atendimentos com snapshot financeiro do serviço. " +
                         "Se 'dataFimRecorrencia' for informado, cria agendamentos recorrentes nos dias da semana especificados " +
                         "(ou no mesmo dia da semana da data inicial se 'diasSemana' não for informado). " +
                         "Sempre retorna uma lista, mesmo para agendamento único."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Atendimento(s) criado(s) com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Paciente ou serviço não encontrado")
    })
    @PostMapping
    public ResponseEntity<List<AtendimentoResponseDTO>> realizarAgendamento(
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados necessários para criar um agendamento (único ou recorrente)") 
            @RequestBody AgendamentoRequestDTO request) {

        log.info("Recebendo requisição de agendamento - Pacientes: {}, Serviço: {}, Data/Hora: {}, Recorrente: {}",
                request.getPacienteIds(), request.getServicoId(), request.getDataHora(),
                request.getDataFimRecorrencia() != null);

        // Chamar o use case (agora sempre retorna lista)
        var atendimentos = request.getPacienteIds().stream()
                .flatMap(pacienteId -> realizarAgendamentoUseCase.realizarAgendamento(
                        pacienteId,
                        request.getServicoId(),
                        request.getDataHora(),
                        request.getDataFimRecorrencia(),
                        request.getDiasSemana(),
                        request.getPacienteIds().size()
                ).stream())
                .toList();

        // Converter para DTOs
        var dtos = atendimentos.stream()
                .map(a -> AtendimentoResponseDTO.builder()
                        .id(a.getId())
                        .pacienteId(a.getPaciente().getId())
                        .servicoBaseId(a.getServicoBase().getId())
                        .dataHoraInicio(a.getDataHoraInicio())
                        .valorCobrado(a.getValorCobrado())
                        .pctClinicaSnapshot(a.getPctClinicaSnapshot())
                        .pctProfissionalSnapshot(a.getPctProfissionalSnapshot())
                        .status(a.getStatus())
                        .build())
                .toList();

        log.info("Agendamento concluído - {} atendimento(s) criado(s)", dtos.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
    }

    @Operation(summary = "Listar atendimentos", description = "Lista com filtros opcionais de data ou paciente")
    @GetMapping
    public ResponseEntity<List<AtendimentoResponseDTO>> listarAtendimentos(
            @RequestParam(required = false) @Parameter(description = "Data/hora inicial (formato: yyyy-MM-dd HH:mm:ss.SSS)", example = "2025-11-01 00:00:00.000") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS") LocalDateTime dataInicio,
            @RequestParam(required = false) @Parameter(description = "Data/hora final (formato: yyyy-MM-dd HH:mm:ss.SSS)", example = "2025-11-30 23:59:59.999") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS") LocalDateTime dataFim,
            @RequestParam(required = false) @Parameter(description = "ID do paciente para filtrar", example = "1") Long pacienteId
    ) {
        var lista = listarAtendimentosUseCase.listar(dataInicio, dataFim, pacienteId);

        var dtos = lista.stream().map(a -> AtendimentoResponseDTO.builder()
                .id(a.getId())
                .pacienteId(a.getPaciente().getId())
                .servicoBaseId(a.getServicoBase().getId())
                .dataHoraInicio(a.getDataHoraInicio())
                .valorCobrado(a.getValorCobrado())
                .pctClinicaSnapshot(a.getPctClinicaSnapshot())
                .pctProfissionalSnapshot(a.getPctProfissionalSnapshot())
                .status(a.getStatus())
                .evolucao(a.getEvolucao())
                .recebedor(a.getRecebedor() != null ? a.getRecebedor().name() : null)
                .tipoPagamento(a.getTipoPagamento() !=  null ? a.getTipoPagamento().name() : null)
                .build()).toList();

        return ResponseEntity.ok(dtos);
    }
    @Operation(summary = "Buscar atendimento por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento encontrado"),
            @ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AtendimentoResponseDTO> buscarPorId(@Parameter(description = "ID do atendimento", example = "1") @PathVariable Long id) {
        var opt = buscarAtendimentoUseCase.buscarPorId(id);
        var atendimento = opt.orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Atendimento não encontrado: " + id));
        var dto = AtendimentoResponseDTO.builder()
                .id(atendimento.getId())
                .pacienteId(atendimento.getPaciente().getId())
                .servicoBaseId(atendimento.getServicoBase().getId())
                .dataHoraInicio(atendimento.getDataHoraInicio())
                .valorCobrado(atendimento.getValorCobrado())
                .pctClinicaSnapshot(atendimento.getPctClinicaSnapshot())
                .pctProfissionalSnapshot(atendimento.getPctProfissionalSnapshot())
                .status(atendimento.getStatus())
                .build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Atualizar atendimento", description = "Atualiza data, status e evolução do atendimento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AtendimentoResponseDTO> atualizarAtendimento(@Parameter(description = "ID do atendimento", example = "1") @PathVariable Long id,
                                                                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Campos opcionais para atualização do atendimento") @RequestBody com.physio.infrastructure.in.web.dto.AtendimentoUpdateRequestDTO request) {
        var domain = com.physio.domain.model.Atendimento.builder()
                .dataHoraInicio(request.getDataHoraInicio())
                .status(request.getStatus())
                .evolucao(request.getEvolucao())
                .recebedor(request.getRecebedor())
                .tipoPagamento(request.getTipoPagamento())
                .build();

        var atualizado = atualizarAtendimentoUseCase.atualizar(id, domain);
        var dto = AtendimentoResponseDTO.builder()
                .id(atualizado.getId())
                .pacienteId(atualizado.getPaciente().getId())
                .servicoBaseId(atualizado.getServicoBase().getId())
                .dataHoraInicio(atualizado.getDataHoraInicio())
                .valorCobrado(atualizado.getValorCobrado())
                .pctClinicaSnapshot(atualizado.getPctClinicaSnapshot())
                .pctProfissionalSnapshot(atualizado.getPctProfissionalSnapshot())
                .status(atualizado.getStatus())
                .build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Deletar atendimento", description = "Remove fisicamente o atendimento do banco")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Atendimento removido"),
            @ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAtendimento(@Parameter(description = "ID do atendimento", example = "1") @PathVariable Long id) {
        deletarAtendimentoUseCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
