package com.physio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrocaPlanoResultado {
    private Assinatura novaAssinatura;
    private boolean cobrancaMesAtualMantida;
}
