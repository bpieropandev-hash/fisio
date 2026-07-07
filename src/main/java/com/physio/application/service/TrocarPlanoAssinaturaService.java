package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.ServicoConfig;
import com.physio.domain.model.TrocaPlanoResultado;
import com.physio.domain.ports.in.TrocarPlanoAssinaturaUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrocarPlanoAssinaturaService implements TrocarPlanoAssinaturaUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final ServicoRepositoryPort servicoRepositoryPort;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Override
    @Transactional
    public TrocaPlanoResultado trocarPlano(Long assinaturaId, Long novoServicoId, BigDecimal novoValorMensal,
                                            Integer diaVencimento, LocalDate dataInicio) {
        log.info("Trocando plano da assinatura - ID: {}, novoServicoId: {}", assinaturaId, novoServicoId);

        Assinatura atual = assinaturaRepositoryPort.buscarPorId(assinaturaId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Assinatura não encontrada: " + assinaturaId));

        if (atual.getAtivo() == null || !atual.getAtivo()) {
            throw new IllegalArgumentException("Assinatura não está ativa: " + assinaturaId);
        }

        if (diaVencimento < 1 || diaVencimento > 28) {
            throw new IllegalArgumentException("Dia de vencimento deve estar entre 1 e 28");
        }

        ServicoConfig novoServico = servicoRepositoryPort.buscarPorIdEAtivo(novoServicoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Serviço não encontrado ou inativo: " + novoServicoId));

        Paciente paciente = atual.getPaciente();

        boolean jaExisteAtivaNoNovoServico = assinaturaRepositoryPort
                .buscarAtivaPorPacienteEServico(paciente.getId().longValue(), novoServicoId)
                .isPresent();

        if (jaExisteAtivaNoNovoServico) {
            throw new IllegalArgumentException(
                    "Já existe uma assinatura ativa para o paciente " + paciente.getId() + " e serviço " + novoServicoId);
        }

        // Verifica se o mês corrente já tem cobrança gerada para a assinatura atual —
        // regra do snapshot: cobranças já geradas nunca mudam sozinhas, só informamos
        LocalDate hoje = LocalDate.now();
        boolean cobrancaMesAtualMantida = cobrancaMensalRepositoryPort
                .buscarPorAssinaturaMesAno(assinaturaId, hoje.getMonthValue(), hoje.getYear())
                .isPresent();

        atual.setAtivo(false);
        atual.setDataCancelamento(LocalDateTime.now());
        assinaturaRepositoryPort.salvar(atual);

        Assinatura nova = Assinatura.builder()
                .paciente(paciente)
                .servico(novoServico)
                .valorMensal(novoValorMensal)
                .diaVencimento(diaVencimento)
                .ativo(true)
                .dataInicio(dataInicio != null ? dataInicio : LocalDate.now())
                .build();

        Assinatura novaSalva = assinaturaRepositoryPort.salvar(nova);

        log.info("Plano trocado com sucesso - Assinatura antiga: {}, nova: {}", assinaturaId, novaSalva.getId());

        return TrocaPlanoResultado.builder()
                .novaAssinatura(novaSalva)
                .cobrancaMesAtualMantida(cobrancaMesAtualMantida)
                .build();
    }
}
