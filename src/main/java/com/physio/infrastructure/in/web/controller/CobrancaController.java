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
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

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

        CobrancaMensalResponseDTO dto = CobrancaMensalResponseDTO.builder()
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

        return ResponseEntity.ok(dto);
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
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada: " + id));

        CobrancaMensalResponseDTO dto = CobrancaMensalResponseDTO.builder()
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

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Listar cobranças de uma assinatura")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada")})
    @GetMapping("/assinatura/{assinaturaId}")
    public ResponseEntity<List<CobrancaMensalResponseDTO>> listarPorAssinatura(
            @Parameter(description = "ID da assinatura", example = "1")
            @PathVariable Long assinaturaId) {
        List<CobrancaMensal> cobrancas = cobrancaMensalRepositoryPort.listarPorAssinatura(assinaturaId);
        List<CobrancaMensalResponseDTO> dtos = cobrancas.stream()
                .map(c -> CobrancaMensalResponseDTO.builder()
                        .id(c.getId())
                        .assinaturaId(c.getAssinatura().getId())
                        .descricao(c.getAssinatura().getPaciente().getNome() + " - " +
                                  c.getAssinatura().getServico().getNome())
                        .mesReferencia(c.getMesReferencia())
                        .anoReferencia(c.getAnoReferencia())
                        .valor(c.getValor())
                        .status(c.getStatus())
                        .dataPagamento(c.getDataPagamento())
                        .recebedor(c.getRecebedor())
                        .tipoPagamento(c.getTipoPagamento())
                        .pctClinicaSnapshot(c.getPctClinicaSnapshot())
                        .pctProfissionalSnapshot(c.getPctProfissionalSnapshot())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}

