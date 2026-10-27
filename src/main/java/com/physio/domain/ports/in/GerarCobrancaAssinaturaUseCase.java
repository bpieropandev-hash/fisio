package com.physio.domain.ports.in;

public interface GerarCobrancaAssinaturaUseCase {
    boolean gerarCobrancaMesAtual(Long assinaturaId);
}
