package com.physio.application.service;

import com.physio.domain.ports.in.GerarCobrancasUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Serviço agendado para gerar automaticamente as cobranças mensais
 * no dia 1º de cada mês às 00:00:00.
 * 
 * Para desabilitar, remova a anotação @Component ou configure
 * spring.task.scheduling.enabled=false no application.properties
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CobrancaSchedulerService {

    private final GerarCobrancasUseCase gerarCobrancasUseCase;

    /**
     * Gera cobranças mensais automaticamente no dia 1º de cada mês às 00:00:00
     * 
     * Cron expression: segundo minuto hora dia mês dia-da-semana
     * 0 0 0 1 * ? = todo dia 1 às 00:00:00
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void gerarCobrancasAutomaticas() {
        LocalDate hoje = LocalDate.now();
        int mes = hoje.getMonthValue();
        int ano = hoje.getYear();

        log.info("Iniciando geração automática de cobranças para {}/{}", mes, ano);

        try {
            int quantidadeGerada = gerarCobrancasUseCase.gerarCobrancasDoMes(mes, ano);
            log.info("Geração automática concluída. {} cobrança(s) gerada(s) para {}/{}", 
                    quantidadeGerada, mes, ano);
        } catch (Exception e) {
            log.error("Erro ao gerar cobranças automaticamente para {}/{}", mes, ano, e);
            // Não relança a exceção para não quebrar o scheduler
        }
    }
}

