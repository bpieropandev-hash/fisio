package com.physio.application.service;

import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.ports.in.ListarCobrancasUseCase;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListarCobrancasService implements ListarCobrancasUseCase {

    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Override
    public List<CobrancaMensal> listar(Integer mes, Integer ano) {
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new IllegalArgumentException("Mês inválido: " + mes + " (use 1 a 12)");
        }
        return cobrancaMensalRepositoryPort.listarPorMesAno(mes, ano);
    }
}
