package com.physio.infrastructure.out.persistence.adapter;

import com.physio.domain.model.Assinatura;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.AssinaturaEntity;
import com.physio.infrastructure.out.persistence.mapper.AssinaturaMapper;
import com.physio.infrastructure.out.persistence.repository.AssinaturaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AssinaturaPersistenceAdapter implements AssinaturaRepositoryPort {

    private final AssinaturaJpaRepository jpaRepository;
    private final AssinaturaMapper mapper;

    @Override
    public Assinatura salvar(Assinatura assinatura) {
        AssinaturaEntity entity = mapper.toEntity(assinatura);
        AssinaturaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Assinatura> buscarPorId(Long id) {
        return jpaRepository.findById(Math.toIntExact(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<Assinatura> listarTodas() {
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
    public Optional<Assinatura> buscarAtivaPorPacienteEServico(Long pacienteId, Long servicoId) {
        return jpaRepository.findAtivaPorPacienteEServico(
                Math.toIntExact(pacienteId),
                Math.toIntExact(servicoId)
        ).map(mapper::toDomain);
    }

    @Override
    public List<Assinatura> listarAtivas() {
        return jpaRepository.findByAtivoTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Assinatura> listarPorPaciente(Long pacienteId) {
        return jpaRepository.findByPaciente_Id(Math.toIntExact(pacienteId)).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

