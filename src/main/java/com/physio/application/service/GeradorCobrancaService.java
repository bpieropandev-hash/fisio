package com.physio.application.service;

import com.physio.domain.model.Assinatura;
import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.ports.in.GerarCobrancasUseCase;
import com.physio.domain.ports.out.AssinaturaRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeradorCobrancaService implements GerarCobrancasUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;

    @Override
    @Transactional
    public int gerarCobrancasDoMes(int mes, int ano) {
        log.info("Iniciando geração de cobranças para mês: {}/{}", mes, ano);

        // Buscar todas as assinaturas ativas
        List<Assinatura> assinaturasAtivas = assinaturaRepositoryPort.listarAtivas();
        log.info("Encontradas {} assinaturas ativas", assinaturasAtivas.size());

        int cobrancasGeradas = 0;

        for (Assinatura assinatura : assinaturasAtivas) {
            // Verificar se já existe cobrança para este mês/ano
            boolean jaExiste = cobrancaMensalRepositoryPort
                    .buscarPorAssinaturaMesAno(
                            Long.valueOf(assinatura.getId()),
                            mes,
                            ano
                    )
                    .isPresent();

            if (jaExiste) {
                log.debug("Cobrança já existe para assinatura ID: {} - mês: {}/{}", 
                        assinatura.getId(), mes, ano);
                continue;
            }

            // Criar nova cobrança mensal
            CobrancaMensal novaCobranca = CobrancaMensal.builder()
                    .assinatura(assinatura)
                    .mesReferencia(mes)
                    .anoReferencia(ano)
                    .valor(assinatura.getValorMensal())
                    .status(StatusCobranca.PENDENTE)
                    .build();

            // Criar snapshot financeiro dos percentuais
            novaCobranca.criarSnapshotFinanceiro(assinatura.getServico());

            // Salvar cobrança
            cobrancaMensalRepositoryPort.salvar(novaCobranca);
            cobrancasGeradas++;

            log.info("Cobrança gerada - Assinatura ID: {}, Valor: R$ {}, Mês: {}/{}",
                    assinatura.getId(),
                    novaCobranca.getValor(),
                    mes,
                    ano);
        }

        log.info("Geração de cobranças concluída. Total gerado: {}", cobrancasGeradas);
        return cobrancasGeradas;
    }
}

