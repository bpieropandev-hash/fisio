package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.in.CriarAssinaturaUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CriarAssinaturaService implements CriarAssinaturaUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ServicoRepositoryPort servicoRepositoryPort;

    @Override
    @Transactional
    public List<Assinatura> criarAssinatura(List<Long> pacienteIds, Long servicoId, BigDecimal valorMensal, Integer diaVencimento, LocalDate dataInicio) {
        log.info("Criando assinaturas - Pacientes: {}, Serviço: {}, Valor: R$ {}, Dia Vencimento: {}",
                pacienteIds, servicoId, valorMensal, diaVencimento);

        // Validação: Verificar se o serviço existe e está ativo (uma vez para todos)
        ServicoConfig servico = servicoRepositoryPort.buscarPorIdEAtivo(servicoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Serviço não encontrado ou inativo: " + servicoId));

        // Validação: Dia de vencimento deve estar entre 1 e 28
        if (diaVencimento < 1 || diaVencimento > 28) {
            throw new IllegalArgumentException("Dia de vencimento deve estar entre 1 e 28");
        }

        List<Assinatura> created = new ArrayList<>();

        for (Long pacienteId : pacienteIds) {
            // Validação: Verificar se o paciente existe e está ativo
            Paciente paciente = pacienteRepositoryPort.buscarPorId(pacienteId)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Paciente não encontrado: " + pacienteId));

            if (paciente.getAtivo() == null || !paciente.getAtivo()) {
                throw new IllegalArgumentException("Paciente não está ativo: " + pacienteId);
            }

            // Validação: Verificar se já existe assinatura ativa para este paciente e serviço
            boolean jaExiste = assinaturaRepositoryPort
                    .buscarAtivaPorPacienteEServico(pacienteId, servicoId)
                    .isPresent();

            if (jaExiste) {
                throw new IllegalArgumentException("Já existe uma assinatura ativa para o paciente " + pacienteId + " e serviço " + servicoId);
            }

            // Criar nova assinatura
            Assinatura assinatura = Assinatura.builder()
                    .paciente(paciente)
                    .servico(servico)
                    .valorMensal(valorMensal)
                    .diaVencimento(diaVencimento)
                    .ativo(true)
                    .dataInicio(dataInicio != null ? dataInicio : LocalDate.now())
                    .build();

            // Salvar assinatura
            Assinatura assinaturaSalva = assinaturaRepositoryPort.salvar(assinatura);
            created.add(assinaturaSalva);

            log.info("Assinatura criada com sucesso - ID: {} (Paciente: {})", assinaturaSalva.getId(), pacienteId);
        }

        return created;
    }
}
