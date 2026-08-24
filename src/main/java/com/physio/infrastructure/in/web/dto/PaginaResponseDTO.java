package com.physio.infrastructure.in.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação para endpoints de listagem (P1.3).
 * Formato próprio em vez de serializar org.springframework.data.domain.Page
 * direto: evita expor a estrutura interna do Spring Data (pageable/sort aninhados).
 */
public record PaginaResponseDTO<T>(
        List<T> conteudo,
        int paginaAtual,
        int tamanhoPagina,
        long totalElementos,
        int totalPaginas
) {
    public static <E, T> PaginaResponseDTO<T> from(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponseDTO<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
