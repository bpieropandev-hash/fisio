package com.physio.infrastructure.out.persistence.adapter;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.mapper.AtendimentoMapper;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AtendimentoPersistenceAdapter implements AtendimentoRepositoryPort {

    private final AtendimentoJpaRepository jpaRepository;
    private final AtendimentoMapper mapper;

    @Override
    public Atendimento salvar(Atendimento atendimento) {
        AtendimentoEntity entity = mapper.toEntity(atendimento);
        AtendimentoEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Atendimento buscarPorId(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain)
                .orElse(null);
    }

    @Override
    public java.util.List<Atendimento> listarTodos() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deletar(Long id) {
        if (id == null) return;
        jpaRepository.deleteById(id);
    }
}
