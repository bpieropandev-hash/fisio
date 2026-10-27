package com.physio.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConfiguracoesDTO(Long versao, String emitenteNome, String emitenteDocumento,
        BigDecimal mensalidadeValorPadrao, Integer mensalidadeDiaVencimento,
        Integer capacidadeFisioterapia, Integer capacidadePilates, Boolean agruparAgenda,
        Integer fotoLimiteMiB, Boolean exibirDor, List<Item> procedimentos, List<Item> equipamentos) {
    public record Item(String codigo, String nome, Boolean ativo) {}
}
