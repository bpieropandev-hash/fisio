package com.physio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {
    private Integer id;
    private String nome;
    private String cpf;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    private String telefone;
    private String email;
    private LocalDateTime dataCadastro;
    private LocalDate dataNascimento;
    private String complemento;
    private String anamnese;
    private Boolean ativo;

}
