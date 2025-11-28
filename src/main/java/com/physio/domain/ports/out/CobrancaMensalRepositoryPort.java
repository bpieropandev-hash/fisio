package com.physio.domain.ports.out;

import com.physio.domain.model.CobrancaMensal;

import java.util.List;
import java.util.Optional;

public interface CobrancaMensalRepositoryPort {
    CobrancaMensal salvar(CobrancaMensal cobrancaMensal);
    Optional<CobrancaMensal> buscarPorId(Long id);
    List<CobrancaMensal> listarTodas();
    void deletar(Long id);
    
    // Verificar se já existe cobrança para uma assinatura em um mês/ano específico
    Optional<CobrancaMensal> buscarPorAssinaturaMesAno(Long assinaturaId, Integer mes, Integer ano);
    
    // Buscar cobranças pagas para relatório financeiro
    List<CobrancaMensal> buscarPagasPorPeriodo(Integer anoInicio, Integer mesInicio, Integer anoFim, Integer mesFim);
    
    // Buscar cobranças de uma assinatura
    List<CobrancaMensal> listarPorAssinatura(Long assinaturaId);
}

