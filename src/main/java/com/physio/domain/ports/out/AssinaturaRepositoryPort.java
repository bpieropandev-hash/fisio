package com.physio.domain.ports.out;

import com.physio.domain.model.Assinatura;

import java.util.List;
import java.util.Optional;

public interface AssinaturaRepositoryPort {
    Assinatura salvar(Assinatura assinatura);
    Optional<Assinatura> buscarPorId(Long id);
    List<Assinatura> listarTodas();
    void deletar(Long id);
    
    // Buscar assinatura ativa de um paciente para um serviço específico
    Optional<Assinatura> buscarAtivaPorPacienteEServico(Long pacienteId, Long servicoId);
    
    // Buscar todas as assinaturas ativas
    List<Assinatura> listarAtivas();
    
    // Buscar assinaturas de um paciente
    List<Assinatura> listarPorPaciente(Long pacienteId);
}

