package com.physio.application.service;

import java.math.BigDecimal;

/** Valida o split final, inclusive após combinar uma atualização parcial. */
final class ValidadorSplitServico {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private ValidadorSplitServico() { }

    static void validar(BigDecimal clinica, BigDecimal profissional) {
        if (clinica == null || profissional == null || clinica.signum() < 0 || profissional.signum() < 0
                || clinica.compareTo(CEM) > 0 || profissional.compareTo(CEM) > 0
                || clinica.add(profissional).compareTo(CEM) != 0) {
            throw new IllegalArgumentException("Os percentuais da clínica e do profissional devem estar entre 0 e 100 e somar 100");
        }
    }
}
