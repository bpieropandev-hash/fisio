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

    @Operation(summary = "Realizar agendamento", description = "Cria um atendimento com snapshot financeiro do serviço")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Atendimento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Paciente ou serviço não encontrado")
    })
    @PostMapping
    public ResponseEntity<AtendimentoResponseDTO> realizarAgendamento(
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados necessários para criar um agendamento") @RequestBody AgendamentoRequestDTO request) {

        log.info("Recebendo requisição de agendamento - Paciente: {}, Serviço: {}, Data/Hora: {}",
                request.getPacienteId(), request.getServicoId(), request.getDataHora());

        Atendimento atendimento = realizarAgendamentoUseCase.realizarAgendamento(
                request.getPacienteId(),
                request.getServicoId(),
                request.getDataHora()
        );

        AtendimentoResponseDTO response = AtendimentoResponseDTO.builder()
                .id(atendimento.getId())
                .pacienteId(atendimento.getPaciente().getId())
                .servicoBaseId(atendimento.getServicoBase().getId())
                .dataHoraInicio(atendimento.getDataHoraInicio())
                .valorCobrado(atendimento.getValorCobrado())
                .pctClinicaSnapshot(atendimento.getPctClinicaSnapshot())
                .pctProfissionalSnapshot(atendimento.getPctProfissionalSnapshot())
                .status(atendimento.getStatus())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar atendimentos", description = "Lista com filtros opcionais de data ou paciente")
    @GetMapping
    public ResponseEntity<List<AtendimentoResponseDTO>> listarAtendimentos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Long pacienteId
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
        var atendimento = opt.orElseThrow(() -> new IllegalArgumentException("Atendimento não encontrado: " + id));
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
