package com.physio.domain.ports.out;

import com.physio.domain.model.CobrancaMensal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    // Listar cobranças filtrando opcionalmente por mês/ano de referência (nulos = sem filtro), paginado (P1.3)
    Page<CobrancaMensal> listarPorMesAno(Integer mes, Integer ano, Pageable pageable);

    // IDs das assinaturas que já têm cobrança gerada num mês/ano, numa query só (P2.4 — evita N+1)
    Set<Integer> buscarAssinaturaIdsComCobranca(Integer mes, Integer ano);
}

