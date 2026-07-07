package com.physio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReajusteResultado {
    private Integer assinaturasAtualizadas;
    private Integer cobrancasAtualizadas;
    private List<Integer> assinaturasIgnoradasIds;
}
