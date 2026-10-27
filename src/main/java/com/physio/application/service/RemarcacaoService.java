package com.physio.application.service;
import com.physio.domain.ports.out.*;
import com.physio.infrastructure.out.persistence.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.physio.domain.model.*;
import com.physio.infrastructure.out.persistence.entity.RemarcacaoEntity;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static com.physio.application.service.RemarcacaoContrato.*;
@Service @RequiredArgsConstructor
public class RemarcacaoService {
 private final AtendimentoRepositoryPort agenda;
 private final ServicoRepositoryPort servicos;
 private final AssinaturaRepositoryPort assinaturas;
 private final ReciboJpaRepository recibos;
 private final RemarcacaoRepository operacoes;
 private final ObjectMapper json;
 private static final ZoneId ZONA=ZoneId.of("America/Sao_Paulo");
 private record Simulacao(Previa previa,List<Atendimento> elegiveis,List<Atendimento> novos,String assinatura) {}

 @Transactional public List<Sessao> candidatos(Long paciente,Long servico,LocalDate inicio,LocalDate fim) {
  periodo(inicio,fim);
  return agenda.candidatosRemarcacao(paciente,servico,inicio.atStartOfDay(),fim.plusDays(1).atStartOfDay()).stream()
   .map(a->new Sessao(a.getId(),a.getDataHoraInicio(),a.getDataHoraFim(),protecao(a))).toList();
 }

 @Transactional public Previa prever(Pedido pedido,String autor) {
  autor(autor);validar(pedido);bloquear(pedido);
  String id=UUID.randomUUID().toString();var s=simular(pedido,id);
  var op=new RemarcacaoEntity();op.setId(id);op.setAutor(autor);op.setMotivo(pedido.motivo().trim());op.setCriadaEm(agora());
  op.setPedido(escrever(pedido));op.setAssinatura(s.assinatura());op.setPrevia(escrever(s.previa()));operacoes.save(op);
  return s.previa();
 }

 @Transactional public Resultado confirmar(String id,String autor) {
  autor(autor);agenda.bloquearAgenda();
  var op=operacoes.findById(id).orElseThrow(()->new IllegalArgumentException("Prévia não encontrada. Gere nova prévia."));
  if(!Objects.equals(op.getAutor(),autor))throw new org.springframework.security.access.AccessDeniedException("Esta prévia pertence a outro usuário.");
  if(op.getResultado()!=null)return ler(op.getResultado(),Resultado.class);
  var pedido=ler(op.getPedido(),Pedido.class);validar(pedido);bloquear(pedido);
  var s=simular(pedido,id);
  if(!Objects.equals(op.getAssinatura(),s.assinatura()))throw new IllegalArgumentException("Agenda, seleção ou regras alteradas. Gere nova prévia antes de confirmar.");
  if(!s.previa().conflitos().isEmpty()||s.elegiveis().isEmpty()||s.novos().isEmpty())throw new IllegalArgumentException("A prévia não permite confirmação. Confira os conflitos e gere nova prévia.");
  List<Integer> destinos=new ArrayList<>();
  if(pedido.modo()==Modo.MANTER_DATAS){
   for(int i=0;i<s.elegiveis().size();i++){
    var a=s.elegiveis().get(i);var novo=s.novos().get(i);
    a.setDataHoraInicio(novo.getDataHoraInicio());a.setDataHoraFim(novo.getDataHoraFim());destinos.add(agenda.salvar(a).getId());
   }
  }else{
   for(var a:s.elegiveis()){a.setStatus(StatusAtendimento.CANCELADO);agenda.salvar(a);}
   for(var novo:s.novos())destinos.add(agenda.salvar(novo).getId());
  }
  var resultado=new Resultado(id,s.elegiveis().stream().map(Atendimento::getId).toList(),List.copyOf(destinos));
  op.setResultado(escrever(resultado));op.setConfirmadaEm(agora());operacoes.save(op);return resultado;
 }

 private void bloquear(Pedido p){
  agenda.bloquearAgenda();operacoes.bloquearServico(p.servicoId());operacoes.bloquearPaciente(p.pacienteId());
  operacoes.bloquearAssinaturas(p.pacienteId(),p.servicoId());
  p.origemIds().stream().sorted().forEach(id->agenda.bloquearAtendimento(id.longValue()));
 }

 private Simulacao simular(Pedido p,String id){
  var servico=servicos.buscarPorIdEAtivo(p.servicoId()).orElseThrow(()->new IllegalArgumentException("Serviço não encontrado ou inativo."));
  List<Atendimento> origens=p.origemIds().stream().sorted().map(i->{
   var a=agenda.buscarPorId(i.longValue());
   if(a==null||a.getPaciente()==null||a.getServicoBase()==null||a.getPaciente().getId().longValue()!=p.pacienteId()||a.getServicoBase().getId().longValue()!=p.servicoId())
    throw new IllegalArgumentException("A seleção contém sessão inexistente, de outro paciente ou serviço: "+i);
   if(a.getDataHoraInicio()==null||a.getDataHoraInicio().toLocalDate().isBefore(p.inicio())||a.getDataHoraInicio().toLocalDate().isAfter(p.fim()))throw new IllegalArgumentException("Sessão fora do período confirmado: "+i);
   if(Boolean.FALSE.equals(a.getPaciente().getAtivo()))throw new IllegalArgumentException("Paciente inativo.");
   return a;
  }).toList();
  List<Sessao> sessoes=origens.stream().map(a->new Sessao(a.getId(),a.getDataHoraInicio(),a.getDataHoraFim(),protecao(a))).toList();
  var idsElegiveis=sessoes.stream().filter(a->a.protecao()==null).map(Sessao::id).collect(java.util.stream.Collectors.toSet());
  var elegiveis=origens.stream().filter(a->idsElegiveis.contains(a.getId())).toList();
  var conflitos=new ArrayList<Conflito>();var novos=new ArrayList<Atendimento>();var vinculadas=new ArrayList<Assinatura>();
  var assinaturaAtual=assinaturas.buscarAtivaPorPacienteEServico(p.pacienteId(),p.servicoId());
  boolean origemMensal=elegiveis.stream().anyMatch(a->a.getAssinaturaId()!=null||a.getModalidadeCobranca()==ModalidadeCobranca.MENSALIDADE);
  if(p.modo()==Modo.MANTER_DATAS){
   for(var a:elegiveis){
    var inicio=a.getDataHoraInicio().toLocalDate().atTime(p.horario());
    var novo=Atendimento.builder().id(a.getId()).paciente(a.getPaciente()).servicoBase(servico).dataHoraInicio(inicio)
     .dataHoraFim(inicio.plus(Duration.between(a.getDataHoraInicio(),a.getDataHoraFim()))).status(StatusAtendimento.AGENDADO)
     .modalidadeCobranca(a.getModalidadeCobranca()).assinaturaId(a.getAssinaturaId()).valorCobrado(a.getValorCobrado()).build();
    if(a.getAssinaturaId()!=null){
     var assinatura=assinaturas.buscarPorId(a.getAssinaturaId().longValue());assinatura.ifPresent(vinculadas::add);
     if(assinatura.isEmpty()||!coberta(assinatura.get(),inicio))conflitos.add(new Conflito(inicio,"Novo horário fora da vigência da mensalidade vinculada."));
    }else if(a.getModalidadeCobranca()==ModalidadeCobranca.MENSALIDADE)conflitos.add(new Conflito(inicio,"Mensalidade sem assinatura vinculada. Regularize a origem."));
    novos.add(novo);
   }
  }else if(!elegiveis.isEmpty()){
   for(var data=p.inicio();!data.isAfter(p.fim());data=data.plusDays(1))if(p.diasSemana().contains(data.getDayOfWeek().getValue())){
    var inicio=data.atTime(p.horario());var novo=Atendimento.builder().paciente(elegiveis.getFirst().getPaciente()).servicoBase(servico)
     .dataHoraInicio(inicio).dataHoraFim(inicio.plusMinutes("Avaliação".equals(servico.getNome())?90:60))
     .status(StatusAtendimento.AGENDADO).statusPagamento(StatusPagamento.PAYMENT_PENDING).build();
    if(assinaturaAtual.filter(a->coberta(a,inicio)).isPresent()){
     novo.setAssinaturaId(assinaturaAtual.get().getId());novo.setModalidadeCobranca(ModalidadeCobranca.MENSALIDADE);
     novo.setValorCobrado(BigDecimal.ZERO);novo.setPctClinicaSnapshot(BigDecimal.ZERO);novo.setPctProfissionalSnapshot(BigDecimal.ZERO);
    }else{
     novo.setModalidadeCobranca(ModalidadeCobranca.AVULSO);novo.criarSnapshotFinanceiro(servico);
     if(origemMensal)conflitos.add(new Conflito(inicio,"Nova sessão sem cobertura vigente: mensalidade não pode virar avulsa silenciosamente."));
    }
    novos.add(novo);
   }
  }
  var fimConsulta=novos.stream().map(Atendimento::getDataHoraFim).max(LocalDateTime::compareTo)
   .filter(fim->fim.isAfter(p.fim().plusDays(1).atStartOfDay())).orElse(p.fim().plusDays(1).atStartOfDay());
  var ocupacao=agenda.listarConflitosPorPeriodo(p.inicio().atStartOfDay(),fimConsulta).stream()
   .sorted(Comparator.comparing(Atendimento::getId)).toList();
  var restantes=ocupacao.stream().filter(a->!idsElegiveis.contains(a.getId())&&a.getStatus()!=StatusAtendimento.CANCELADO).toList();
  for(int i=0;i<novos.size();i++){
   var novo=novos.get(i);String causa=null;
   if(!novo.getDataHoraInicio().isAfter(agora()))causa="O novo horário já passou.";
   var sobrepostos=new ArrayList<Atendimento>();
   for(var a:restantes)if(sobrepoe(a,novo))sobrepostos.add(a);
   for(int j=0;j<novos.size();j++)if(i!=j&&sobrepoe(novos.get(j),novo))sobrepostos.add(novos.get(j));
   if(sobrepostos.stream().anyMatch(a->Objects.equals(a.getPaciente().getId(),p.pacienteId().intValue())))causa="Paciente já possui atendimento neste horário.";
   else if(sobrepostos.stream().anyMatch(a->!Objects.equals(a.getServicoBase().getId(),servico.getId())))causa="Serviço incompatível no mesmo horário.";
   else if(sobrepostos.size()+1>ValidadorCapacidade.capacidade(servico))causa="Capacidade máxima neste horário atingida ("+ValidadorCapacidade.capacidade(servico)+" pacientes).";
   if(causa!=null)conflitos.add(new Conflito(novo.getDataHoraInicio(),causa));
  }
  int avulsas=p.modo()==Modo.SUBSTITUIR_DIAS?(int)novos.stream().filter(a->a.getModalidadeCobranca()==ModalidadeCobranca.AVULSO).count():0;
  var valor=avulsas==0?BigDecimal.ZERO:novos.stream().filter(a->a.getModalidadeCobranca()==ModalidadeCobranca.AVULSO).map(Atendimento::getValorCobrado).reduce(BigDecimal.ZERO,BigDecimal::add);
  int freqAntes=(int)elegiveis.stream().map(a->a.getDataHoraInicio().getDayOfWeek()).distinct().count();
  var previa=new Previa(id,sessoes,novos.stream().map(a->new Destino(a.getId(),a.getDataHoraInicio(),a.getDataHoraFim(),Objects.toString(a.getModalidadeCobranca(),"LEGADO"),a.getValorCobrado())).toList(),List.copyOf(conflitos),elegiveis.size(),origens.size()-elegiveis.size(),p.modo()==Modo.MANTER_DATAS?elegiveis.size():0,p.modo()==Modo.SUBSTITUIR_DIAS?elegiveis.size():0,p.modo()==Modo.SUBSTITUIR_DIAS?novos.size():0,Math.max(0,novos.size()-elegiveis.size()),freqAntes,p.modo()==Modo.MANTER_DATAS?freqAntes:new HashSet<>(p.diasSemana()).size(),avulsas,valor);
  String assinatura=hash(escrever(Arrays.asList(origens,sessoes,ocupacao,servico,assinaturaAtual.orElse(null),vinculadas,previa)));
  return new Simulacao(previa,elegiveis,novos,assinatura);
 }
 private String protecao(Atendimento a){
  var causas=new ArrayList<String>();
  if(a.getDataHoraInicio()==null||!a.getDataHoraInicio().isAfter(agora()))causas.add("Horário passado");
  if(a.getStatus()!=StatusAtendimento.AGENDADO)causas.add("Status "+a.getStatus());
  if(a.getEvolucao()!=null&&!a.getEvolucao().isBlank()||agenda.possuiEvolucaoEstruturada(a.getId().longValue()))causas.add("Evolução clínica registrada");
  if(a.getStatusPagamento()==StatusPagamento.PAID||a.getDataPagamento()!=null||a.getRecebedor()!=null||a.getTipoPagamento()!=null)causas.add("Recebimento registrado");
  if(recibos.findByAtendimentoId(a.getId()).isPresent())causas.add("Recibo emitido");
  if(a.getDataHoraFim()==null||a.getDataHoraInicio()==null||!a.getDataHoraFim().isAfter(a.getDataHoraInicio()))causas.add("Duração da sessão precisa de regularização");
  return causas.isEmpty()?null:String.join("; ",causas);
 }
 private boolean coberta(Assinatura a,LocalDateTime inicio){return !Boolean.FALSE.equals(a.getAtivo())&&a.getDataInicio()!=null&&!inicio.toLocalDate().isBefore(a.getDataInicio())&&(a.getDataCancelamento()==null||inicio.isBefore(a.getDataCancelamento()));}
 private boolean sobrepoe(Atendimento a,Atendimento b){return a.getDataHoraInicio()!=null&&a.getDataHoraFim()!=null&&a.getDataHoraInicio().isBefore(b.getDataHoraFim())&&a.getDataHoraFim().isAfter(b.getDataHoraInicio());}
 private void periodo(LocalDate inicio,LocalDate fim){if(inicio==null||fim==null||inicio.isBefore(agora().toLocalDate())||fim.isBefore(inicio)||ChronoUnit.DAYS.between(inicio,fim)>=366)throw new IllegalArgumentException("Informe período futuro, ordenado e de até 366 dias.");}
 private void validar(Pedido p){
  if(p==null)throw new IllegalArgumentException("Informe a programação.");periodo(p.inicio(),p.fim());
  if(p.pacienteId()==null||p.servicoId()==null||p.pacienteId()<1||p.servicoId()<1||p.horario()==null||p.modo()==null)throw new IllegalArgumentException("Informe paciente, serviço, modo e horário.");
  if(p.origemIds()==null||p.origemIds().isEmpty()||p.origemIds().stream().anyMatch(i->i==null||i<1)||new HashSet<>(p.origemIds()).size()!=p.origemIds().size())throw new IllegalArgumentException("Selecione explicitamente os IDs de origem, sem repetição.");
  if(p.motivo()==null||p.motivo().trim().length()<3||p.motivo().length()>1000)throw new IllegalArgumentException("Informe motivo entre 3 e 1000 caracteres.");
  if(p.modo()==Modo.SUBSTITUIR_DIAS&&(p.diasSemana()==null||p.diasSemana().isEmpty()||p.diasSemana().stream().anyMatch(d->d==null||d<1||d>7)||new HashSet<>(p.diasSemana()).size()!=p.diasSemana().size()))throw new IllegalArgumentException("Selecione dias da semana válidos e sem repetição.");
 }
 private void autor(String autor){if(autor==null||autor.isBlank()||autor.length()>255)throw new IllegalArgumentException("Autor inválido.");}
 private LocalDateTime agora(){return LocalDateTime.now(ZONA);}
 private String escrever(Object valor){try{return json.writeValueAsString(valor);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Falha ao registrar auditoria da remarcação",e);}}
 private <T>T ler(String valor,Class<T> tipo){try{return json.readValue(valor,tipo);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Auditoria da remarcação inválida",e);}}
 private String hash(String valor){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
