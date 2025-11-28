package com.physio.infrastructure.in.web.controller;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.in.CriarPacienteUseCase;
import com.physio.infrastructure.in.web.dto.PacienteCreateRequestDTO;
import com.physio.infrastructure.in.web.dto.PacienteResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/pacientes")
@Tag(name = "Pacientes", description = "Operações para gerenciar pacientes (CRUD)")
@RequiredArgsConstructor
public class PacienteController {

    private final CriarPacienteUseCase criarPacienteUseCase;
    private final com.physio.domain.ports.in.BuscarPacienteUseCase buscarPacienteUseCase;
    private final com.physio.domain.ports.in.ListarPacientesUseCase listarPacientesUseCase;
    private final com.physio.domain.ports.in.AtualizarPacienteUseCase atualizarPacienteUseCase;

    @Operation(summary = "Criar paciente")
    @ApiResponse(responseCode = "201", description = "Paciente criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<PacienteResponseDTO> criarPaciente(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Payload para criar paciente") @RequestBody PacienteCreateRequestDTO request) {

        Paciente domain = Paciente.builder()
                .nome(request.getNome())
                .cpf(request.getCpf())
                .dataNascimento(request.getDataNascimento())
                .telefone(request.getTelefone())
                .email(request.getEmail())
                .logradouro(request.getLogradouro())
                .numero(request.getNumero())
                .bairro(request.getBairro())
                .cidade(request.getCidade())
                .estado(request.getEstado())
                .cep(request.getCep())
                .complemento(request.getComplemento())
                .anamnese(request.getAnamnese())
                .build();

        Paciente salvo = criarPacienteUseCase.criarPaciente(domain);

        PacienteResponseDTO response = PacienteResponseDTO.builder()
                .id(salvo.getId())
                .nome(salvo.getNome())
                .cpf(salvo.getCpf())
                .dataNascimento(salvo.getDataNascimento())
                .telefone(salvo.getTelefone())
                .email(salvo.getEmail())
                .logradouro(salvo.getLogradouro())
                .numero(salvo.getNumero())
                .bairro(salvo.getBairro())
                .cidade(salvo.getCidade())
                .estado(salvo.getEstado())
                .cep(salvo.getCep())
                .complemento(salvo.getComplemento())
                .anamnese(salvo.getAnamnese())
                .dataCadastro(salvo.getDataCadastro())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar pacientes")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    @GetMapping
    public ResponseEntity<List<PacienteResponseDTO>> listarPacientes() {
        var lista = listarPacientesUseCase.listarTodos();
        var dtos = lista.stream().map(p -> PacienteResponseDTO.builder()
                .id(p.getId())
                .nome(p.getNome())
                .cpf(p.getCpf())
                .dataNascimento(p.getDataNascimento())
                .telefone(p.getTelefone())
                .email(p.getEmail())
                .logradouro(p.getLogradouro())
                .numero(p.getNumero())
                .bairro(p.getBairro())
                .cidade(p.getCidade())
                .estado(p.getEstado())
                .cep(p.getCep())
                .complemento(p.getComplemento())
                .anamnese(p.getAnamnese())
                .dataCadastro(p.getDataCadastro())
                .ativo(p.getAtivo())
                .build()).toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Buscar paciente por ID")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> buscarPorId(@Parameter(description = "ID do paciente", example = "1") @PathVariable Long id) {
        var opt = buscarPacienteUseCase.buscarPorId(id);
        var paciente = opt.orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado: " + id));
        var dto = PacienteResponseDTO.builder()
                .id(paciente.getId())
                .nome(paciente.getNome())
                .cpf(paciente.getCpf())
                .dataNascimento(paciente.getDataNascimento())
                .telefone(paciente.getTelefone())
                .email(paciente.getEmail())
                .logradouro(paciente.getLogradouro())
                .numero(paciente.getNumero())
                .bairro(paciente.getBairro())
                .cidade(paciente.getCidade())
                .estado(paciente.getEstado())
                .cep(paciente.getCep())
                .complemento(paciente.getComplemento())
                .anamnese(paciente.getAnamnese())
                .dataCadastro(paciente.getDataCadastro())
                .ativo(paciente.getAtivo())
                .build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Atualizar paciente")
    @ApiResponse(responseCode = "200", description = "Paciente atualizado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> atualizarPaciente(@Parameter(description = "ID do paciente", example = "1") @PathVariable Long id, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Payload de atualização") @RequestBody PacienteCreateRequestDTO request) {
        var domain = Paciente.builder()
                .nome(request.getNome())
                .cpf(request.getCpf())
                .dataNascimento(request.getDataNascimento())
                .telefone(request.getTelefone())
                .email(request.getEmail())
                .logradouro(request.getLogradouro())
                .numero(request.getNumero())
                .bairro(request.getBairro())
                .cidade(request.getCidade())
                .estado(request.getEstado())
                .cep(request.getCep())
                .complemento(request.getComplemento())
                .anamnese(request.getAnamnese())
                .build();

        var atualizado = atualizarPacienteUseCase.atualizar(id, domain);
        var dto = PacienteResponseDTO.builder()
                .id(atualizado.getId())
                .nome(atualizado.getNome())
                .cpf(atualizado.getCpf())
                .dataNascimento(atualizado.getDataNascimento())
                .telefone(atualizado.getTelefone())
                .email(atualizado.getEmail())
                .logradouro(atualizado.getLogradouro())
                .numero(atualizado.getNumero())
                .bairro(atualizado.getBairro())
                .cidade(atualizado.getCidade())
                .estado(atualizado.getEstado())
                .cep(atualizado.getCep())
                .complemento(atualizado.getComplemento())
                .anamnese(atualizado.getAnamnese())
                .dataCadastro(atualizado.getDataCadastro())
                .ativo(atualizado.getAtivo())
                .build();
        return ResponseEntity.ok(dto);
    }
}
