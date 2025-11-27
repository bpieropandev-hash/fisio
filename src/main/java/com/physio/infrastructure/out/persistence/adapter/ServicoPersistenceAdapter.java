package com.physio.infrastructure.out.persistence.adapter;

import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import com.physio.infrastructure.out.persistence.mapper.ServicoConfigMapper;
import com.physio.infrastructure.out.persistence.repository.ServicoConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServicoPersistenceAdapter implements ServicoRepositoryPort {

    private final ServicoConfigJpaRepository jpaRepository;
    private final ServicoConfigMapper mapper;

    @Override
    public Optional<ServicoConfig> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        Integer intId = id.intValue();
        return jpaRepository.findById(intId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ServicoConfig> buscarPorIdEAtivo(Long id) {
        if (id == null) return Optional.empty();
        Integer intId = id.intValue();
        return jpaRepository.findByIdAndAtivoTrue(intId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ServicoConfig> buscarPorNome(String nome) {
        return jpaRepository.findByNome(nome)
                .map(mapper::toDomain);
    }

    @Override
    public ServicoConfig salvar(ServicoConfig servico) {
        var entity = mapper.toEntity(servico);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ServicoConfig> listarTodos() {
        var list = jpaRepository.findAll();
        return list.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void desativar(Long id) {
        if (id == null) return;
        Integer intId = id.intValue();
        jpaRepository.findById(intId).ifPresent(entity -> {
            entity.setAtivo(false);
            jpaRepository.save(entity);
        });
    }
}
