package com.physio.infrastructure.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteCreateRequestDTO {
    @NotBlank
    @Size(max = 255)
    @Schema(example = "João da Silva")
    private String nome;

    @NotBlank
    @Size(max = 11)
    @Schema(example = "12345678901")
    private String cpf;

    private LocalDate dataNascimento;
    private String telefone;
    private String email;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    private String complemento;
    private String anamnese;
}
