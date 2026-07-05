package com.physio.infrastructure.in.web.controller;

import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.ports.in.AtualizarCobrancaUseCase;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.in.web.dto.CobrancaMensalResponseDTO;
import com.physio.infrastructure.in.web.dto.CobrancaMensalUpdateRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/cobrancas")
@Tag(name = "Cobranças Mensais", description = "Operações para gerenciar cobranças mensais")
@RequiredArgsConstructor
public class CobrancaController {

    private final AtualizarCobrancaUseCase atualizarCobrancaUseCase;
    private final com.physio.domain.ports.in.ListarCobrancasUseCase listarCobrancasUseCase;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Operation(summary = "Listar cobranças mensais",
            description = "Lista todas as cobranças, com filtro opcional por mês e/ou ano de referência")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada")})
    @GetMapping
    public ResponseEntity<List<CobrancaMensalResponseDTO>> listar(
            @Parameter(description = "Mês de referência (1-12)", example = "7")
            @RequestParam(required = false) Integer mes,
            @Parameter(description = "Ano de referência", example = "2026")
            @RequestParam(required = false) Integer ano) {
        List<CobrancaMensalResponseDTO> dtos = listarCobrancasUseCase.listar(mes, ano).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Atualizar cobrança mensal", description = "Atualiza o status de uma cobrança (marcar como paga)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cobrança atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cobrança não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CobrancaMensalResponseDTO> atualizarCobranca(
            @Parameter(description = "ID da cobrança", example = "1")
            @PathVariable Long id,
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados de atualização")
            @RequestBody CobrancaMensalUpdateRequestDTO request) {
        log.info("Atualizando cobrança {} - Status: {}", id, request.getStatus());

        CobrancaMensal cobranca = atualizarCobrancaUseCase.atualizarCobranca(
                id,
                request.getStatus(),
                request.getDataPagamento(),
                request.getRecebedor(),
                request.getTipoPagamento()
        );

        return ResponseEntity.ok(toDTO(cobranca));
    }

    @Operation(summary = "Buscar cobrança por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cobrança encontrada"),
            @ApiResponse(responseCode = "404", description = "Cobrança não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CobrancaMensalResponseDTO> buscarPorId(
            @Parameter(description = "ID da cobrança", example = "1")
            @PathVariable Long id) {
        CobrancaMensal cobranca = cobrancaMensalRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cobrança não encontrada: " + id));

        return ResponseEntity.ok(toDTO(cobranca));
    }

    @Operation(summary = "Listar cobranças de uma assinatura")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada")})
    @GetMapping("/assinatura/{assinaturaId}")
    public ResponseEntity<List<CobrancaMensalResponseDTO>> listarPorAssinatura(
            @Parameter(description = "ID da assinatura", example = "1")
            @PathVariable Long assinaturaId) {
        List<CobrancaMensal> cobrancas = cobrancaMensalRepositoryPort.listarPorAssinatura(assinaturaId);
        List<CobrancaMensalResponseDTO> dtos = cobrancas.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private CobrancaMensalResponseDTO toDTO(CobrancaMensal cobranca) {
        return CobrancaMensalResponseDTO.builder()
                .id(cobranca.getId())
                .assinaturaId(cobranca.getAssinatura().getId())
                .descricao(cobranca.getAssinatura().getPaciente().getNome() + " - " +
                          cobranca.getAssinatura().getServico().getNome())
                .mesReferencia(cobranca.getMesReferencia())
                .anoReferencia(cobranca.getAnoReferencia())
                .valor(cobranca.getValor())
                .status(cobranca.getStatus())
                .dataPagamento(cobranca.getDataPagamento())
                .recebedor(cobranca.getRecebedor())
                .tipoPagamento(cobranca.getTipoPagamento())
                .pctClinicaSnapshot(cobranca.getPctClinicaSnapshot())
                .pctProfissionalSnapshot(cobranca.getPctProfissionalSnapshot())
                .build();
    }
}
