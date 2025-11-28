package com.physio.infrastructure.in.web.controller;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.in.BuscarAssinaturaUseCase;
import com.physio.domain.ports.in.CriarAssinaturaUseCase;
import com.physio.infrastructure.in.web.dto.AssinaturaCreateRequestDTO;
import com.physio.infrastructure.in.web.dto.AssinaturaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/assinaturas")
@Tag(name = "Assinaturas", description = "Operações para gerenciar assinaturas mensais")
@RequiredArgsConstructor
public class AssinaturaController {

    private final CriarAssinaturaUseCase criarAssinaturaUseCase;
    private final BuscarAssinaturaUseCase buscarAssinaturaUseCase;

    @Operation(summary = "Criar assinatura", description = "Cria uma nova assinatura mensal para um paciente e serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assinatura criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou já existe assinatura ativa")
    })
    @PostMapping
    public ResponseEntity<AssinaturaResponseDTO> criarAssinatura(
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados da assinatura")
            @RequestBody AssinaturaCreateRequestDTO request) {
        log.info("Criando assinatura - Paciente: {}, Serviço: {}", request.getPacienteId(), request.getServicoId());

        Assinatura assinatura = criarAssinaturaUseCase.criarAssinatura(
                request.getPacienteId(),
                request.getServicoId(),
                request.getValorMensal(),
                request.getDiaVencimento(),
                request.getDataInicio()
        );

        AssinaturaResponseDTO response = AssinaturaResponseDTO.builder()
                .id(assinatura.getId())
                .pacienteId(assinatura.getPaciente().getId())
                .pacienteNome(assinatura.getPaciente().getNome())
                .servicoId(assinatura.getServico().getId())
                .servicoNome(assinatura.getServico().getNome())
                .valorMensal(assinatura.getValorMensal())
                .diaVencimento(assinatura.getDiaVencimento())
                .ativo(assinatura.getAtivo())
                .dataInicio(assinatura.getDataInicio())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar todas as assinaturas")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada")})
    @GetMapping
    public ResponseEntity<List<AssinaturaResponseDTO>> listarTodas() {
        List<Assinatura> assinaturas = buscarAssinaturaUseCase.listarTodas();
        List<AssinaturaResponseDTO> dtos = assinaturas.stream()
                .map(a -> AssinaturaResponseDTO.builder()
                        .id(a.getId())
                        .pacienteId(a.getPaciente().getId())
                        .pacienteNome(a.getPaciente().getNome())
                        .servicoId(a.getServico().getId())
                        .servicoNome(a.getServico().getNome())
                        .valorMensal(a.getValorMensal())
                        .diaVencimento(a.getDiaVencimento())
                        .ativo(a.getAtivo())
                        .dataInicio(a.getDataInicio())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Buscar assinatura por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura encontrada"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AssinaturaResponseDTO> buscarPorId(
            @Parameter(description = "ID da assinatura", example = "1")
            @PathVariable Long id) {
        Assinatura assinatura = buscarAssinaturaUseCase.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada: " + id));

        AssinaturaResponseDTO dto = AssinaturaResponseDTO.builder()
                .id(assinatura.getId())
                .pacienteId(assinatura.getPaciente().getId())
                .pacienteNome(assinatura.getPaciente().getNome())
                .servicoId(assinatura.getServico().getId())
                .servicoNome(assinatura.getServico().getNome())
                .valorMensal(assinatura.getValorMensal())
                .diaVencimento(assinatura.getDiaVencimento())
                .ativo(assinatura.getAtivo())
                .dataInicio(assinatura.getDataInicio())
                .build();

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Listar assinaturas de um paciente")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada")})
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<AssinaturaResponseDTO>> listarPorPaciente(
            @Parameter(description = "ID do paciente", example = "1")
            @PathVariable Long pacienteId) {
        List<Assinatura> assinaturas = buscarAssinaturaUseCase.listarPorPaciente(pacienteId);
        List<AssinaturaResponseDTO> dtos = assinaturas.stream()
                .map(a -> AssinaturaResponseDTO.builder()
                        .id(a.getId())
                        .pacienteId(a.getPaciente().getId())
                        .pacienteNome(a.getPaciente().getNome())
                        .servicoId(a.getServico().getId())
                        .servicoNome(a.getServico().getNome())
                        .valorMensal(a.getValorMensal())
                        .diaVencimento(a.getDiaVencimento())
                        .ativo(a.getAtivo())
                        .dataInicio(a.getDataInicio())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}

