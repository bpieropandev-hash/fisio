package com.physio.infrastructure.out.persistence.adapter;

import com.physio.domain.model.Paciente;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import com.physio.infrastructure.out.persistence.mapper.PacienteMapper;
import com.physio.infrastructure.out.persistence.repository.PacienteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PacientePersistenceAdapter implements PacienteRepositoryPort {

    private final PacienteJpaRepository jpaRepository;
    private final PacienteMapper mapper;

    @Override
    public Optional<Paciente> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        Integer intId = id.intValue();
        return jpaRepository.findById(intId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Paciente> buscarPorCpf(String cpf) {
        return jpaRepository.findByCpf(cpf)
                .map(mapper::toDomain);
    }

    @Override
    public Paciente salvar(Paciente paciente) {
        var entity = mapper.toEntity(paciente);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public java.util.List<Paciente> listarTodos() {
        var list = jpaRepository.findAll();
        return list.stream().map(mapper::toDomain).toList();
    }
}
