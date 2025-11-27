package com.physio.infrastructure.in.web.controller;

import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.CriarServicoUseCase;
import com.physio.domain.ports.in.BuscarServicoUseCase;
import com.physio.domain.ports.in.ListarServicosUseCase;
import com.physio.domain.ports.in.AtualizarServicoUseCase;
import com.physio.domain.ports.in.DeletarServicoUseCase;
import com.physio.infrastructure.in.web.dto.ServicoCreateRequestDTO;
import com.physio.infrastructure.in.web.dto.ServicoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/servicos")
@Tag(name = "Serviços", description = "Operações para gerenciar serviços (CRUD)")
@RequiredArgsConstructor
public class ServicoController {

    private final CriarServicoUseCase criarServicoUseCase;
    private final BuscarServicoUseCase buscarServicoUseCase;
    private final ListarServicosUseCase listarServicosUseCase;
    private final AtualizarServicoUseCase atualizarServicoUseCase;
    private final DeletarServicoUseCase deletarServicoUseCase;

    @Operation(summary = "Criar serviço", description = "Cria um novo serviço com valores e percentuais")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<ServicoResponseDTO> criarServico(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Payload para criar serviço") @RequestBody ServicoCreateRequestDTO request) {
        log.info("Recebendo requisição de criação de serviço - Nome: {}", request.getNome());

        ServicoConfig domain = ServicoConfig.builder()
                .nome(request.getNome())
                .valorBase(request.getValorBase())
                .pctClinica(request.getPctClinica())
                .pctProfissional(request.getPctProfissional())
                .ativo(request.getAtivo())
                .build();

        ServicoConfig salvo = criarServicoUseCase.criarServico(domain);

        ServicoResponseDTO response = ServicoResponseDTO.builder()
                .id(salvo.getId())
                .nome(salvo.getNome())
                .valorBase(salvo.getValorBase())
                .pctClinica(salvo.getPctClinica())
                .pctProfissional(salvo.getPctProfissional())
                .ativo(salvo.getAtivo())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar serviços", description = "Retorna todos os serviços")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada")})
    @GetMapping
    public ResponseEntity<?> listarServicos() {
        var lista = listarServicosUseCase.listarTodos();
        var dtoList = lista.stream().map(s -> ServicoResponseDTO.builder()
                .id(s.getId())
                .nome(s.getNome())
                .valorBase(s.getValorBase())
                .pctClinica(s.getPctClinica())
                .pctProfissional(s.getPctProfissional())
                .ativo(s.getAtivo())
                .build()).toList();
        return ResponseEntity.ok(dtoList);
    }

    @Operation(summary = "Buscar serviço por ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Serviço encontrado"), @ApiResponse(responseCode = "404", description = "Serviço não encontrado")})
    @GetMapping("/{id}")
    public ResponseEntity<ServicoResponseDTO> buscarPorId(@Parameter(description = "ID do serviço", example = "1") @PathVariable Long id) {
        var opt = buscarServicoUseCase.buscarPorId(id);
        var servico = opt.orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + id));
        var dto = ServicoResponseDTO.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .valorBase(servico.getValorBase())
                .pctClinica(servico.getPctClinica())
                .pctProfissional(servico.getPctProfissional())
                .ativo(servico.getAtivo())
                .build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Atualizar serviço")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Serviço atualizado"), @ApiResponse(responseCode = "404", description = "Serviço não encontrado")})
    @PutMapping("/{id}")
    public ResponseEntity<ServicoResponseDTO> atualizarServico(@Parameter(description = "ID do serviço", example = "1") @PathVariable Long id, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Payload de atualização") @RequestBody ServicoCreateRequestDTO request) {
        ServicoConfig domain = ServicoConfig.builder()
                .nome(request.getNome())
                .valorBase(request.getValorBase())
                .pctClinica(request.getPctClinica())
                .pctProfissional(request.getPctProfissional())
                .ativo(request.getAtivo())
                .build();

        var atualizado = atualizarServicoUseCase.atualizar(id, domain);
        var dto = ServicoResponseDTO.builder()
                .id(atualizado.getId())
                .nome(atualizado.getNome())
                .valorBase(atualizado.getValorBase())
                .pctClinica(atualizado.getPctClinica())
                .pctProfissional(atualizado.getPctProfissional())
                .ativo(atualizado.getAtivo())
                .build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Desativar serviço", description = "Marca o serviço como inativo (soft-delete)")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Serviço desativado")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarServico(@Parameter(description = "ID do serviço", example = "1") @PathVariable Long id) {
        deletarServicoUseCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
