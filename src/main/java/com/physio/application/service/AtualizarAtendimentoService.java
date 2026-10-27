package com.physio.application.service;
import com.physio.domain.model.*;
import com.physio.domain.ports.in.AtualizarAtendimentoUseCase;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class AtualizarAtendimentoService implements AtualizarAtendimentoUseCase {
 private final AtendimentoRepositoryPort atendimentoRepositoryPort;
 private final com.physio.domain.ports.out.AssinaturaRepositoryPort assinaturas;
 @Override @Transactional public Atendimento atualizar(Long id, Atendimento dados) {
  atendimentoRepositoryPort.bloquearAgenda();
  atendimentoRepositoryPort.bloquearAtendimento(id);
  var atual=atendimentoRepositoryPort.buscarPorId(id);
  if(atual==null) throw new jakarta.persistence.EntityNotFoundException("Atendimento não encontrado: "+id);
  boolean mudouHorario=dados.getDataHoraInicio()!=null||dados.getDataHoraFim()!=null || (atual.getStatus()==StatusAtendimento.CANCELADO && dados.getStatus()!=null && dados.getStatus()!=StatusAtendimento.CANCELADO);
  if(mudouHorario && atual.getAssinaturaId()!=null) {
   var assinatura=assinaturas.buscarPorId(atual.getAssinaturaId().longValue()).orElseThrow(()->new IllegalArgumentException("Assinatura vinculada não encontrada"));
   var novoInicio=dados.getDataHoraInicio()!=null?dados.getDataHoraInicio():atual.getDataHoraInicio();
   if(assinatura.getDataInicio()==null || novoInicio.toLocalDate().isBefore(assinatura.getDataInicio()) || (assinatura.getDataCancelamento()!=null && !novoInicio.isBefore(assinatura.getDataCancelamento()))) throw new IllegalArgumentException("O novo horário está fora da vigência da mensalidade");
  }
  if(dados.getDataHoraInicio()!=null) {
   if(dados.getDataHoraFim()==null && atual.getDataHoraFim()!=null) atual.setDataHoraFim(dados.getDataHoraInicio().plus(java.time.Duration.between(atual.getDataHoraInicio(),atual.getDataHoraFim())));
   atual.setDataHoraInicio(dados.getDataHoraInicio());
  }
  if(dados.getDataHoraFim()!=null) atual.setDataHoraFim(dados.getDataHoraFim());
  if(dados.getStatus()!=null) atual.setStatus(dados.getStatus());
  if(mudouHorario && atual.getStatus()!=StatusAtendimento.CANCELADO) ValidadorCapacidade.validar(atendimentoRepositoryPort,atual.getServicoBase(),atual.getPaciente().getId(),atual.getDataHoraInicio(),atual.getDataHoraFim(),atual.getId());
  if(dados.getEvolucao()!=null) {
   if(atendimentoRepositoryPort.possuiEvolucaoEstruturada(id) && !java.util.Objects.equals(dados.getEvolucao(),atual.getEvolucao())) throw new IllegalArgumentException("Atualize a evolução pela ficha estruturada");
   atual.setEvolucao(dados.getEvolucao());
  }
  if(atual.getModalidadeCobranca()==ModalidadeCobranca.MENSALIDADE || atual.getAssinaturaId()!=null) {
   if(dados.getRecebedor()!=null || dados.getTipoPagamento()!=null || dados.getDataPagamento()!=null) throw new IllegalArgumentException("Atendimento coberto por mensalidade: registre pagamento na cobrança mensal");
  } else {
   if(dados.getRecebedor()!=null) atual.setRecebedor(dados.getRecebedor());
   if(dados.getTipoPagamento()!=null) atual.setTipoPagamento(dados.getTipoPagamento());
   if(dados.getDataPagamento()!=null) {
    if(dados.getDataPagamento().isAfter(java.time.LocalDate.now())) throw new IllegalArgumentException("Data de pagamento não pode ser futura");
    atual.setDataPagamento(dados.getDataPagamento());
   }
   if(atual.getRecebedor()!=null && atual.getTipoPagamento()!=null) {
    if(atual.getDataPagamento()==null && atual.getStatusPagamento()!=StatusPagamento.PAID) throw new IllegalArgumentException("Informe a data de recebimento");
    atual.setStatusPagamento(StatusPagamento.PAID);
   } else atual.setStatusPagamento(StatusPagamento.PAYMENT_PENDING);
  }
  return atendimentoRepositoryPort.salvar(atual);
 }
}
