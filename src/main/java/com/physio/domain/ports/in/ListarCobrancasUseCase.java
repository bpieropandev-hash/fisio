package com.physio.domain.ports.in;

import com.physio.domain.model.CobrancaMensal;

import java.util.List;

public interface ListarCobrancasUseCase {
    /**
     * Lista cobranças mensais, opcionalmente filtradas por mês e/ou ano de referência.
     * Parâmetros nulos são ignorados (null/null = todas).
     */
    List<CobrancaMensal> listar(Integer mes, Integer ano);
}
