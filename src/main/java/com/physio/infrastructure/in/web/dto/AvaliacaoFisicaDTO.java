package com.physio.infrastructure.in.web.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record AvaliacaoFisicaDTO(Long id, Integer pacienteId, LocalDate dataAvaliacao, String titulo,
                                 String observacao, LocalDateTime criadoEm, String autor,
                                 String nomeArquivo, String tipoConteudo) {}
