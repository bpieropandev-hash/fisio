package com.physio.domain.ports.in;

import com.physio.domain.model.CobrancaMensal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListarCobrancasUseCase {
    /**
     * Lista cobranças mensais, opcionalmente filtradas por mês e/ou ano de referência.
     * Parâmetros nulos são ignorados (null/null = todas). Paginado (P1.3).
     */
    Page<CobrancaMensal> listar(Integer mes, Integer ano, Pageable pageable);
}
