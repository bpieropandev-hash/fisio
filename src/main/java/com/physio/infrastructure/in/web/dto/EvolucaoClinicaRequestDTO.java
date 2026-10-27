package com.physio.infrastructure.in.web.dto;
import java.util.List;
public record EvolucaoClinicaRequestDTO(Integer versao, List<String> procedimentos, List<String> equipamentos,
        Integer pressaoSistolica, Integer pressaoDiastolica, Integer dor, String observacoes) {}
