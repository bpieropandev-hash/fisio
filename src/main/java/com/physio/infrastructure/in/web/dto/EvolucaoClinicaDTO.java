package com.physio.infrastructure.in.web.dto;
import java.time.LocalDateTime;
import java.util.List;
public record EvolucaoClinicaDTO(int versao, List<String> procedimentos, List<String> equipamentos,
        Integer pressaoSistolica, Integer pressaoDiastolica, Integer dor, String observacoes,
        String resumo, String autor, LocalDateTime atualizadoEm) {}
