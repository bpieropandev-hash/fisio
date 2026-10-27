package com.physio.application.service;

import com.physio.infrastructure.out.persistence.repository.ServicoConfigJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AtualizarCapacidadeServicoService {
    private final ServicoConfigJpaRepository repository;

    @Transactional
    public void atualizar(Integer id, Integer capacidade) {
        if(capacidade==null || capacidade<1 || capacidade>100)
            throw new IllegalArgumentException("Capacidade deve estar entre 1 e 100");
        if(repository.atualizarCapacidade(id,capacidade)==0)
            throw new EntityNotFoundException("Serviço não encontrado: "+id);
    }
}
