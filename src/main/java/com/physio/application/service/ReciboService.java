package com.physio.application.service;
import com.physio.domain.model.*;
import com.physio.domain.ports.out.*;
import com.physio.infrastructure.in.web.dto.*;
import com.physio.infrastructure.out.persistence.entity.ReciboEntity;
import com.physio.infrastructure.out.persistence.repository.ReciboJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Service
public class ReciboService {
 private final AtendimentoRepositoryPort atendimentos;
 private final CobrancaMensalRepositoryPort cobrancas;
 private final ReciboJpaRepository recibos;
 private final AgendaTransacaoLock lock;
 private final ConfiguracoesService configuracoes;
 public ReciboService(AtendimentoRepositoryPort atendimentos,CobrancaMensalRepositoryPort cobrancas,ReciboJpaRepository recibos,AgendaTransacaoLock lock,
  ConfiguracoesService configuracoes) {
  this.atendimentos=atendimentos;this.cobrancas=cobrancas;this.recibos=recibos;this.lock=lock;this.configuracoes=configuracoes;
 }
 private void origem(Integer atendimento,Integer cobranca) {
  if((atendimento==null)==(cobranca==null) || atendimento!=null&&atendimento<1 || cobranca!=null&&cobranca<1) throw new IllegalArgumentException("Informe exatamente uma origem: atendimentoId ou cobrancaId");
 }
 @Transactional public ReciboResponseDTO emitir(ReciboRequestDTO request) {
  origem(request.atendimentoId(),request.cobrancaId());
  lock.bloquear();
  var existente=request.atendimentoId()!=null?recibos.findByAtendimentoId(request.atendimentoId()):recibos.findByCobrancaId(request.cobrancaId());
  if(existente.isPresent()) return dto(existente.get());
  var config=configuracoes.buscar();
  String emitenteNome=config.emitenteNome(),emitenteDocumento=config.emitenteDocumento();
  if(emitenteNome==null||emitenteNome.isBlank()||emitenteDocumento==null||emitenteDocumento.isBlank()) throw new IllegalArgumentException("Configure nome e documento do emitente antes de emitir recibos");
  if(request.pagadorNome()==null||request.pagadorNome().isBlank()||request.pagadorNome().length()>255||request.pagadorDocumento()==null||request.pagadorDocumento().isBlank()||request.pagadorDocumento().length()>30) throw new IllegalArgumentException("Informe nome e documento do pagador");
  Paciente paciente;BigDecimal valor;LocalDate pagamento;TipoPagamento tipo;String descricao;
  if(request.atendimentoId()!=null) {
   atendimentos.bloquearAtendimento(request.atendimentoId().longValue());
   var a=atendimentos.buscarPorId(request.atendimentoId().longValue());
   if(a==null) throw new EntityNotFoundException("Atendimento não encontrado");
   if(a.getStatusPagamento()!=StatusPagamento.PAID||a.getAssinaturaId()!=null||a.getModalidadeCobranca()==ModalidadeCobranca.MENSALIDADE) throw new IllegalArgumentException("Recibo exige atendimento avulso com recebimento confirmado");
   paciente=a.getPaciente();valor=a.getValorCobrado();pagamento=a.getDataPagamento();tipo=a.getTipoPagamento();
   descricao="Atendimento - "+a.getServicoBase().getNome()+(a.getDataHoraInicio()!=null?" em "+a.getDataHoraInicio().toLocalDate():"");
  } else {
   cobrancas.bloquear(request.cobrancaId().longValue());
   var c=cobrancas.buscarPorId(request.cobrancaId().longValue()).orElseThrow(()->new EntityNotFoundException("Cobrança não encontrada"));
   if(c.getStatus()!=StatusCobranca.PAGO) throw new IllegalArgumentException("Recibo exige mensalidade com recebimento confirmado");
   paciente=c.getAssinatura().getPaciente();valor=c.getValor();pagamento=c.getDataPagamento();tipo=c.getTipoPagamento();
   descricao="Mensalidade - "+c.getAssinatura().getServico().getNome()+" - "+c.getMesReferencia()+"/"+c.getAnoReferencia();
  }
  if(valor==null||valor.signum()<=0) throw new IllegalArgumentException("Recibo exige valor positivo");
  if(pagamento==null) throw new IllegalArgumentException("Regularize a data de recebimento na origem antes de emitir o recibo");
  if(request.dataPagamento()==null||!pagamento.equals(request.dataPagamento())||pagamento.isAfter(LocalDate.now())) throw new IllegalArgumentException("Data informada deve ser igual à data do recebimento confirmado");
  var entity=ReciboEntity.builder().numero("REC-"+UUID.randomUUID()).atendimentoId(request.atendimentoId()).cobrancaId(request.cobrancaId())
   .pacienteNome(paciente.getNome()).pacienteDocumento(paciente.getCpf()).pagadorNome(request.pagadorNome().trim()).pagadorDocumento(request.pagadorDocumento().trim())
   .emitenteNome(emitenteNome.trim()).emitenteDocumento(emitenteDocumento.trim()).descricao(descricao).valor(valor).dataPagamento(pagamento)
   .tipoPagamento(tipo!=null?tipo.name():null).emitidoEm(LocalDateTime.now()).build();
  return dto(recibos.save(entity));
 }
 @Transactional(readOnly=true) public List<ReciboResponseDTO> listar(Integer atendimentoId,Integer cobrancaId) {
  origem(atendimentoId,cobrancaId);
  return (atendimentoId!=null?recibos.findByAtendimentoId(atendimentoId):recibos.findByCobrancaId(cobrancaId)).stream().map(this::dto).toList();
 }
 @Transactional(readOnly=true) public byte[] pdf(Long id) {
  var r=recibos.findById(id).orElseThrow(()->new EntityNotFoundException("Recibo não encontrado"));
  var output=new java.io.ByteArrayOutputStream();var document=new com.lowagie.text.Document();
  com.lowagie.text.pdf.PdfWriter.getInstance(document,output);document.open();
  for(String linha:List.of("RECIBO INTERNO",r.getNumero(),"Emitente: "+r.getEmitenteNome()+" - "+r.getEmitenteDocumento(),
   "Recebemos de: "+r.getPagadorNome()+" - "+r.getPagadorDocumento(),"Paciente: "+r.getPacienteNome()+" - "+Objects.toString(r.getPacienteDocumento(),""),
   r.getDescricao(),"Valor recebido: R$ "+r.getValor().toPlainString(),"Data do recebimento: "+r.getDataPagamento(),"Forma: "+Objects.toString(r.getTipoPagamento(),""),
   "Emitido em: "+r.getEmitidoEm(),"Recibo interno. Não substitui eventual emissão obrigatória no Receita Saúde.")) document.add(new com.lowagie.text.Paragraph(linha));
  document.close();return output.toByteArray();
 }
 private ReciboResponseDTO dto(ReciboEntity r) { return new ReciboResponseDTO(r.getId(),r.getNumero(),r.getAtendimentoId(),r.getCobrancaId(),r.getPacienteNome(),r.getPacienteDocumento(),r.getPagadorNome(),r.getPagadorDocumento(),r.getEmitenteNome(),r.getEmitenteDocumento(),r.getDescricao(),r.getValor(),r.getDataPagamento(),r.getTipoPagamento(),r.getEmitidoEm()); }
}
