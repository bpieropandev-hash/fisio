package com.physio.application.service;

import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.StatusCobranca;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.EstornoCobrancaEntity;
import com.physio.infrastructure.out.persistence.repository.EstornoCobrancaRepository;
import com.physio.infrastructure.out.persistence.repository.ReciboJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class EstornarCobrancaService {
 private final CobrancaMensalRepositoryPort cobrancas;
 private final ReciboJpaRepository recibos;
 private final EstornoCobrancaRepository estornos;
 private final AgendaTransacaoLock lock;

 @Transactional
 public CobrancaMensal estornar(Long id, String motivo, String autor) {
  if (motivo == null || motivo.isBlank() || motivo.trim().length() > 1000) throw new IllegalArgumentException("Informe o motivo do estorno (até 1000 caracteres)");
  if (autor == null || autor.isBlank()) throw new IllegalArgumentException("Autor do estorno é obrigatório");
  // Mesma ordem de bloqueio da emissão de recibos, impedindo corrida entre operações.
  lock.bloquear();
  cobrancas.bloquear(id);
  var cobranca = cobrancas.buscarPorId(id).orElseThrow(() -> new EntityNotFoundException("Cobrança não encontrada: " + id));
  if (recibos.findByCobrancaId(cobranca.getId()).isPresent()) throw new IllegalArgumentException("Cobrança com recibo emitido não pode ser estornada");
  if (cobranca.getStatus() != StatusCobranca.PAGO) throw new IllegalArgumentException("Somente cobrança paga pode ser estornada");
  estornos.save(EstornoCobrancaEntity.builder().cobrancaId(cobranca.getId()).motivo(motivo.trim()).autor(autor)
   .estornadoEm(LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))).dataPagamentoAnterior(cobranca.getDataPagamento())
   .recebedorAnterior(cobranca.getRecebedor() == null ? null : cobranca.getRecebedor().name())
   .tipoPagamentoAnterior(cobranca.getTipoPagamento() == null ? null : cobranca.getTipoPagamento().name()).build());
  cobranca.setStatus(StatusCobranca.PENDENTE);
  cobranca.setDataPagamento(null);
  cobranca.setRecebedor(null);
  cobranca.setTipoPagamento(null);
  return cobrancas.salvar(cobranca);
 }
}
