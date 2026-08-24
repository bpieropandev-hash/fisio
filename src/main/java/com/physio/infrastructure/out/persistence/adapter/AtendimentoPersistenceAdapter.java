package com.physio.infrastructure.out.persistence.adapter;

import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.mapper.AtendimentoMapper;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    // Cap de segurança (P1.3): a Agenda chama isso sem filtro de data por padrão
    // (comportamento atual, não muda), então sem teto a query cresce sem limite
    // junto com o histórico. 5000 é folgado pro volume real de uma clínica pequena;
    // se algum dia estourar, aí sim vira busca por período de verdade na Agenda.
    private static final int TETO_LISTAR_TODOS = 5000;

    @Override
    public java.util.List<Atendimento> listarTodos() {
        var pageable = PageRequest.of(0, TETO_LISTAR_TODOS, Sort.by(Sort.Direction.DESC, "dataHoraInicio"));
        return jpaRepository.findAll(pageable).stream().map(mapper::toDomain).toList();
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
    public List<Atendimento> listarConflitosPorPeriodo(
            LocalDateTime inicio,
            LocalDateTime fim) {

        return jpaRepository.findConflitosPeriodo(inicio, fim)
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
