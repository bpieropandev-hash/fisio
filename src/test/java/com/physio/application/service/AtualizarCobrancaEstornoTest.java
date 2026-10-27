package com.physio.application.service;

import com.physio.domain.model.*;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.*;
import com.physio.infrastructure.out.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtualizarCobrancaEstornoTest {
 private final CobrancaMensalRepositoryPort repo = mock(CobrancaMensalRepositoryPort.class);
 private final ReciboJpaRepository recibos = mock(ReciboJpaRepository.class);
 private final EstornoCobrancaRepository auditoria = mock(EstornoCobrancaRepository.class);
 private final AgendaTransacaoLock lock = mock(AgendaTransacaoLock.class);
 private final CobrancaMensal cobranca = CobrancaMensal.builder().id(1).status(StatusCobranca.PAGO).dataPagamento(LocalDate.now()).recebedor(Recebedor.CLINICA).tipoPagamento(TipoPagamento.PIX).build();
 private void preparar() { when(repo.buscarPorId(1L)).thenReturn(Optional.of(cobranca)); }
 private EstornarCobrancaService estorno() { return new EstornarCobrancaService(repo, recibos, auditoria, lock); }
 @Test void putLegadoNaoPodeEstornarPagamentoSemAuditoria() {
  preparar();
  var service = new AtualizarCobrancaService(repo, recibos, lock);
  assertThrows(IllegalArgumentException.class, () -> service.atualizarCobranca(1L, StatusCobranca.PENDENTE, null, null, null));
  verify(repo, never()).salvar(any());
  assertEquals(StatusCobranca.PAGO, cobranca.getStatus());
 }
 @Test void estornaLimpaPagamentoRegistraMotivoAutorDataESemDuplicar() {
  preparar(); when(repo.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));
  var pagamento = cobranca.getDataPagamento();
  var result = estorno().estornar(1L,"  Lançamento incorreto  ","profissional");
  assertEquals(StatusCobranca.PENDENTE,result.getStatus());
  assertNull(result.getDataPagamento()); assertNull(result.getRecebedor()); assertNull(result.getTipoPagamento());
  var captor=ArgumentCaptor.forClass(EstornoCobrancaEntity.class);
  verify(auditoria).save(captor.capture());
  assertEquals("Lançamento incorreto",captor.getValue().getMotivo());
  assertEquals("profissional",captor.getValue().getAutor());
  assertEquals(pagamento,captor.getValue().getDataPagamentoAnterior());
  assertNotNull(captor.getValue().getEstornadoEm());
  assertThrows(IllegalArgumentException.class,()->estorno().estornar(1L,"Repetição","profissional"));
  verify(auditoria,times(1)).save(any()); verify(repo,times(1)).salvar(any());
  var ordem=inOrder(lock,repo); ordem.verify(lock).bloquear(); ordem.verify(repo).bloquear(1L);
 }
 @Test void reciboBloqueiaEstornoEAlteracaoLegadaSemModificarOrigem() {
  preparar(); when(recibos.findByCobrancaId(1)).thenReturn(Optional.of(new ReciboEntity()));
  assertThrows(IllegalArgumentException.class,()->estorno().estornar(1L,"Correção","profissional"));
  assertThrows(IllegalArgumentException.class,()->new AtualizarCobrancaService(repo,recibos,lock).atualizarCobranca(1L,StatusCobranca.PENDENTE,null,null,null));
  assertEquals(StatusCobranca.PAGO,cobranca.getStatus()); assertNotNull(cobranca.getDataPagamento());
  verify(repo,never()).salvar(any()); verify(auditoria,never()).save(any());
 }
 @Test void motivoInvalidoNaoGrava() {
  assertThrows(IllegalArgumentException.class,()->estorno().estornar(1L," ","profissional"));
  assertThrows(IllegalArgumentException.class,()->estorno().estornar(1L,"x".repeat(1001),"profissional"));
  verifyNoInteractions(repo,auditoria,recibos,lock);
 }
}
