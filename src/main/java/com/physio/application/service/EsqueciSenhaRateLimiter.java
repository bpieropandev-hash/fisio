package com.physio.application.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Limite simples em memória: evita spam de email de redefinição de senha.
 * Suficiente para o porte atual (1-3 usuários); reavaliar se escalar.
 */
@Component
public class EsqueciSenhaRateLimiter {

    private static final int MAX_TENTATIVAS = 3;
    private static final long JANELA_SEGUNDOS = 3600;

    private final ConcurrentHashMap<String, Deque<Instant>> tentativasPorChave = new ConcurrentHashMap<>();

    public boolean permitir(String chave) {
        Instant agora = Instant.now();
        Deque<Instant> tentativas = tentativasPorChave.computeIfAbsent(chave, k -> new ConcurrentLinkedDeque<>());

        synchronized (tentativas) {
            while (!tentativas.isEmpty() && tentativas.peekFirst().isBefore(agora.minusSeconds(JANELA_SEGUNDOS))) {
                tentativas.pollFirst();
            }
            if (tentativas.size() >= MAX_TENTATIVAS) {
                return false;
            }
            tentativas.addLast(agora);
            return true;
        }
    }
}
