package com.physio.application.service;
import com.physio.domain.model.*;
import com.physio.domain.ports.out.*;
import com.physio.infrastructure.in.web.dto.ReciboRequestDTO;
import com.physio.infrastructure.out.persistence.entity.ReciboEntity;
import com.physio.infrastructure.out.persistence.repository.ReciboJpaRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ReciboServiceTest {
 final AtendimentoRepositoryPort atendimentos=mock(AtendimentoRepositoryPort.class);
 final CobrancaMensalRepositoryPort cobrancas=mock(CobrancaMensalRepositoryPort.class);
 final ReciboJpaRepository recibos=mock(ReciboJpaRepository.class);
 final AgendaTransacaoLock lock=mock(AgendaTransacaoLock.class);
 final LocalDate data=LocalDate.of(2026,10,26);
 final ReciboService service=new ReciboService(atendimentos,cobrancas,recibos,lock,"Clínica Teste","12345678901");
 final ReciboRequestDTO request=new ReciboRequestDTO(1,null,data,"Pagador","12345678901");
 Atendimento atendimento(StatusPagamento status,String valor) {
  return Atendimento.builder().id(1).paciente(Paciente.builder().id(4).nome("Paciente").cpf("11122233344").build())
   .servicoBase(ServicoConfig.builder().id(1).nome("Fisioterapia").build()).valorCobrado(new BigDecimal(valor))
   .statusPagamento(status).dataPagamento(data).tipoPagamento(TipoPagamento.PIX).modalidadeCobranca(ModalidadeCobranca.AVULSO).build();
 }
 @Test void rejeitaReciboDePagamentoPendente() {
  when(atendimentos.buscarPorId(1L)).thenReturn(atendimento(StatusPagamento.PAYMENT_PENDING,"100"));
  assertThrows(IllegalArgumentException.class,()->service.emitir(request)); verify(recibos,never()).save(any());
 }
 @Test void rejeitaReciboDeValorZero() {
  when(atendimentos.buscarPorId(1L)).thenReturn(atendimento(StatusPagamento.PAID,"0"));
  assertThrows(IllegalArgumentException.class,()->service.emitir(request));
 }
 @Test void aceitaUmaUnicaOrigem() {
  assertThrows(IllegalArgumentException.class,()->service.emitir(new ReciboRequestDTO(1,2,data,"Nome","123")));
  assertThrows(IllegalArgumentException.class,()->service.emitir(new ReciboRequestDTO(null,null,data,"Nome","123")));
 }
 @Test void pagoLegadoSemDataPrecisaRegularizarOrigem() {
  var origem=atendimento(StatusPagamento.PAID,"100"); origem.setDataPagamento(null);
  when(atendimentos.buscarPorId(1L)).thenReturn(origem);
  assertThrows(IllegalArgumentException.class,()->service.emitir(request));
 }
 @Test void reciboPagoPreservaSnapshotEReemissaoIdempotente() {
  var origem=atendimento(StatusPagamento.PAID,"100"); when(atendimentos.buscarPorId(1L)).thenReturn(origem);
  when(recibos.save(any())).thenAnswer(i->{ReciboEntity e=i.getArgument(0);e.setId(10L);return e;});
  var emitido=service.emitir(request);
  assertEquals("Paciente",emitido.pacienteNome()); assertEquals("Clínica Teste",emitido.emitenteNome()); assertEquals(new BigDecimal("100"),emitido.valor());
  var captor=org.mockito.ArgumentCaptor.forClass(ReciboEntity.class);verify(recibos).save(captor.capture());
  when(recibos.findByAtendimentoId(1)).thenReturn(Optional.of(captor.getValue()));
  origem.getPaciente().setNome("Alterado");origem.setValorCobrado(BigDecimal.ONE);
  var reemitido=service.emitir(new ReciboRequestDTO(1,null,data,"Outro","999"));
  assertEquals(emitido,reemitido);verify(recibos,times(1)).save(any());
  when(recibos.findById(10L)).thenReturn(Optional.of(captor.getValue()));
  assertTrue(new String(service.pdf(10L),java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF"));
 }
 @Test void rejeitaDataDivergenteDoRecebimento() {
  when(atendimentos.buscarPorId(1L)).thenReturn(atendimento(StatusPagamento.PAID,"100"));
  assertThrows(IllegalArgumentException.class,()->service.emitir(new ReciboRequestDTO(1,null,data.minusDays(1),"Pagador","123")));
 }
 @Test void mensalidadePagaEmiteReciboSemContarSessaoComoAvulso() {
  var assinatura=Assinatura.builder().id(3).paciente(Paciente.builder().id(4).nome("Paciente").cpf("111").build())
    .servico(ServicoConfig.builder().nome("Pilates").build()).build();
  when(cobrancas.buscarPorId(2L)).thenReturn(Optional.of(CobrancaMensal.builder().id(2).assinatura(assinatura)
    .status(StatusCobranca.PAGO).dataPagamento(data).tipoPagamento(TipoPagamento.PIX).valor(new BigDecimal("300"))
    .mesReferencia(10).anoReferencia(2026).build()));
  when(recibos.save(any())).thenAnswer(i->{ReciboEntity e=i.getArgument(0);e.setId(11L);return e;});
  var recibo=service.emitir(new ReciboRequestDTO(null,2,data,"Pagador","123"));
  assertEquals(2,recibo.cobrancaId());assertNull(recibo.atendimentoId());assertEquals(new BigDecimal("300"),recibo.valor());
 }
}
