package com.physio.infrastructure.out.persistence.adapter;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.mapper.AtendimentoMapper;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
        return jpaRepository.findById(Math.toIntExact(id))
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
        jpaRepository.deleteById(Math.toIntExact(id));
    }

    // --- NOVOS MÉTODOS ---
    @Override
    public List<Atendimento> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return jpaRepository.findByDataHoraInicioBetween(inicio, fim)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Atendimento> listarPorPaciente(Long pacienteId) {
        return jpaRepository.findByPaciente_Id(pacienteId.intValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
