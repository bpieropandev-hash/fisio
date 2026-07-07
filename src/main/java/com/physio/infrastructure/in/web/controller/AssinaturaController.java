package com.physio.infrastructure.in.web.controller;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.in.BuscarAssinaturaUseCase;
import com.physio.domain.ports.in.CriarAssinaturaUseCase;
import com.physio.domain.ports.in.CancelarAssinaturaUseCase;
import com.physio.domain.ports.in.AtualizarAssinaturaUseCase;
import com.physio.domain.ports.in.ReativarAssinaturaUseCase;
import com.physio.domain.ports.in.TrocarPlanoAssinaturaUseCase;
import com.physio.domain.model.TrocaPlanoResultado;
import com.physio.infrastructure.in.web.dto.AssinaturaCreateRequestDTO;
import com.physio.infrastructure.in.web.dto.AssinaturaUpdateRequestDTO;
import com.physio.infrastructure.in.web.dto.AssinaturaTrocarPlanoRequestDTO;
import com.physio.infrastructure.in.web.dto.AssinaturaTrocaPlanoResponseDTO;
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
    private final CancelarAssinaturaUseCase cancelarAssinaturaUseCase;
    private final AtualizarAssinaturaUseCase atualizarAssinaturaUseCase;
    private final ReativarAssinaturaUseCase reativarAssinaturaUseCase;
    private final TrocarPlanoAssinaturaUseCase trocarPlanoAssinaturaUseCase;

    @Operation(summary = "Criar assinaturas", description = "Cria novas assinaturas mensais para um ou mais pacientes e um serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assinatura criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou já existe assinatura ativa")
    })
    @PostMapping
    public ResponseEntity<List<AssinaturaResponseDTO>> criarAssinatura(
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados da assinatura")
            @RequestBody AssinaturaCreateRequestDTO request) {
        log.info("Criando assinaturas - Pacientes: {}, Serviço: {}", request.getPacienteIds(), request.getServicoId());

        List<Assinatura> assinaturas = criarAssinaturaUseCase.criarAssinatura(
                request.getPacienteIds(),
                request.getServicoId(),
                request.getValorMensal(),
                request.getDiaVencimento(),
                request.getDataInicio()
        );

        List<AssinaturaResponseDTO> responses = assinaturas.stream()
                .map(assinatura -> AssinaturaResponseDTO.builder()
                        .id(assinatura.getId())
                        .pacienteId(assinatura.getPaciente().getId())
                        .pacienteNome(assinatura.getPaciente().getNome())
                        .servicoId(assinatura.getServico().getId())
                        .servicoNome(assinatura.getServico().getNome())
                        .valorMensal(assinatura.getValorMensal())
                        .diaVencimento(assinatura.getDiaVencimento())
                        .ativo(assinatura.getAtivo())
                        .dataInicio(assinatura.getDataInicio())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
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
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Assinatura não encontrada: " + id));

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

    @Operation(summary = "Cancelar assinatura")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Assinatura cancelada"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")})
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable Long id) {
        cancelarAssinaturaUseCase.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Editar assinatura", description = "Atualiza valor mensal e/ou dia de vencimento de uma assinatura ativa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou assinatura cancelada"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AssinaturaResponseDTO> atualizarAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable Long id,
            @Valid @RequestBody AssinaturaUpdateRequestDTO request) {
        Assinatura assinatura = atualizarAssinaturaUseCase.atualizar(id, request.getValorMensal(), request.getDiaVencimento());

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
                .dataCancelamento(assinatura.getDataCancelamento())
                .build();

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Reativar assinatura", description = "Reativa uma assinatura cancelada, se não houver outra ativa para o mesmo paciente e serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura reativada"),
            @ApiResponse(responseCode = "400", description = "Já existe assinatura ativa para paciente/serviço"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    @PutMapping("/{id}/reativar")
    public ResponseEntity<AssinaturaResponseDTO> reativarAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable Long id) {
        Assinatura assinatura = reativarAssinaturaUseCase.reativar(id);

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
                .dataCancelamento(assinatura.getDataCancelamento())
                .build();

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Trocar plano da assinatura", description = "Cancela a assinatura atual e cria uma nova para o serviço informado, na mesma transação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano trocado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, assinatura cancelada ou já existe assinatura ativa no novo serviço"),
            @ApiResponse(responseCode = "404", description = "Assinatura ou serviço não encontrado")
    })
    @PostMapping("/{id}/trocar-plano")
    public ResponseEntity<AssinaturaTrocaPlanoResponseDTO> trocarPlano(
            @Parameter(description = "ID da assinatura atual") @PathVariable Long id,
            @Valid @RequestBody AssinaturaTrocarPlanoRequestDTO request) {
        TrocaPlanoResultado resultado = trocarPlanoAssinaturaUseCase.trocarPlano(
                id,
                request.getNovoServicoId(),
                request.getNovoValorMensal(),
                request.getDiaVencimento(),
                request.getDataInicio()
        );

        Assinatura nova = resultado.getNovaAssinatura();
        AssinaturaResponseDTO novaDto = AssinaturaResponseDTO.builder()
                .id(nova.getId())
                .pacienteId(nova.getPaciente().getId())
                .pacienteNome(nova.getPaciente().getNome())
                .servicoId(nova.getServico().getId())
                .servicoNome(nova.getServico().getNome())
                .valorMensal(nova.getValorMensal())
                .diaVencimento(nova.getDiaVencimento())
                .ativo(nova.getAtivo())
                .dataInicio(nova.getDataInicio())
                .build();

        AssinaturaTrocaPlanoResponseDTO dto = AssinaturaTrocaPlanoResponseDTO.builder()
                .novaAssinatura(novaDto)
                .cobrancaMesAtualMantida(resultado.isCobrancaMesAtualMantida())
                .build();

        return ResponseEntity.ok(dto);
    }
}
