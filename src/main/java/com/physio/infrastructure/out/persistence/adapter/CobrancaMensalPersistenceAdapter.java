package com.physio.infrastructure.out.persistence.adapter;

import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.CobrancaMensalEntity;
import com.physio.infrastructure.out.persistence.mapper.CobrancaMensalMapper;
import com.physio.infrastructure.out.persistence.repository.CobrancaMensalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CobrancaMensalPersistenceAdapter implements CobrancaMensalRepositoryPort {

    private final CobrancaMensalJpaRepository jpaRepository;
    private final CobrancaMensalMapper mapper;

    @Override
    public CobrancaMensal salvar(CobrancaMensal cobrancaMensal) {
        CobrancaMensalEntity entity = mapper.toEntity(cobrancaMensal);
        CobrancaMensalEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<CobrancaMensal> buscarPorId(Long id) {
        return jpaRepository.findById(Math.toIntExact(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<CobrancaMensal> listarTodas() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deletar(Long id) {
        if (id == null) return;
        jpaRepository.deleteById(Math.toIntExact(id));
    }

    @Override
    public Optional<CobrancaMensal> buscarPorAssinaturaMesAno(Long assinaturaId, Integer mes, Integer ano) {
        return jpaRepository.findByAssinatura_IdAndMesReferenciaAndAnoReferencia(
                Math.toIntExact(assinaturaId),
                mes,
                ano
        ).map(mapper::toDomain);
    }

    @Override
    public List<CobrancaMensal> buscarPagasPorPeriodo(Integer anoInicio, Integer mesInicio, Integer anoFim, Integer mesFim) {
        return jpaRepository.findPagasPorPeriodo(anoInicio, mesInicio, anoFim, mesFim).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CobrancaMensal> listarPorAssinatura(Long assinaturaId) {
        return jpaRepository.findByAssinatura_Id(Math.toIntExact(assinaturaId)).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

