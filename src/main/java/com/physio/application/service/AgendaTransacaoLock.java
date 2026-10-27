package com.physio.application.service;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class AgendaTransacaoLock {
 private final EntityManager entityManager;
 public void bloquear() { entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(7261026)").getSingleResult(); }
}
