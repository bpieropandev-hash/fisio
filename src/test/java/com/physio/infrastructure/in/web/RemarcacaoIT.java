package com.physio.infrastructure.in.web;

import com.fasterxml.jackson.databind.*;
import com.physio.application.service.*;
import com.physio.domain.model.*;
import com.physio.infrastructure.out.persistence.adapter.AtendimentoPersistenceAdapter;
import com.physio.infrastructure.out.persistence.entity.*;
import com.physio.infrastructure.out.persistence.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.physio.application.service.RemarcacaoContrato.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"api.security.token.secret=integration-remarcacao-secret-with-thirty-two-characters","spring.jpa.show-sql=false"})
@Testcontainers
class RemarcacaoIT {
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 private static final AtomicInteger SEQ=new AtomicInteger();
 @Autowired TestRestTemplate http; @Autowired ObjectMapper json; @Autowired UsuarioJpaRepository usuarios;
 @Autowired PasswordEncoder passwords; @Autowired PacienteJpaRepository pacientes; @Autowired ServicoConfigJpaRepository servicos;
 @Autowired AtendimentoJpaRepository atendimentos; @Autowired RemarcacaoRepository operacoes; @Autowired RemarcacaoService service;
 @MockitoSpyBean AtendimentoPersistenceAdapter agenda;
 private String token,autor;private LocalDate dia;private PacienteEntity paciente;private ServicoConfigEntity servico;private AtendimentoEntity origem;

 @BeforeEach void preparar()throws Exception{
  int n=SEQ.incrementAndGet();autor="remarcacao"+n;var u=new UsuarioEntity();u.setLogin(autor);u.setSenha(passwords.encode("test-password"));u.setRole("ADMIN");usuarios.save(u);
  var login=http.postForEntity("/api/auth/login",Map.of("login",autor,"senha","test-password"),String.class);token=json.readTree(login.getBody()).path("token").asText();
  dia=LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(20L*n);
  paciente=pacientes.save(PacienteEntity.builder().nome("Paciente remarcação "+n).ativo(true).build());
  servico=servicos.save(ServicoConfigEntity.builder().nome("Pilates remarcação "+n).tipo(TipoServico.PILATES).capacidade(3).ativo(true).valorBase(new BigDecimal("100")).pctClinica(new BigDecimal("40")).pctProfissional(new BigDecimal("60")).build());
  origem=atendimentos.save(AtendimentoEntity.builder().paciente(paciente).servicoBase(servico).dataHoraInicio(dia.atTime(15,0)).dataHoraFim(dia.atTime(16,30)).status(StatusAtendimento.AGENDADO).statusPagamento(StatusPagamento.PAYMENT_PENDING).modalidadeCobranca(ModalidadeCobranca.AVULSO).valorCobrado(new BigDecimal("70")).pctClinicaSnapshot(new BigDecimal("25")).pctProfissionalSnapshot(new BigDecimal("75")).build());
 }
 private Pedido pedido(Modo modo){return new Pedido(paciente.getId().longValue(),servico.getId().longValue(),List.of(origem.getId()),dia,dia.plusDays(6),modo,LocalTime.of(18,0),List.of(2,3),"Mudança de disponibilidade");}
 private ResponseEntity<String> request(String path,Object body){var headers=new HttpHeaders();headers.setBearerAuth(token);headers.setContentType(MediaType.APPLICATION_JSON);return http.exchange(path,body==null?HttpMethod.GET:HttpMethod.POST,new HttpEntity<>(body,headers),String.class);}
 private JsonNode ok(String path,Object body)throws Exception{var r=request(path,body);assertThat(r.getStatusCode().value()).as(r.getBody()).isEqualTo(200);return json.readTree(r.getBody());}

 @Test void endpointsMantemIdDuracaoSnapshotEReplay()throws Exception{
  var candidatos=ok("/api/v1/remarcacoes/candidatos?pacienteId="+paciente.getId()+"&servicoId="+servico.getId()+"&inicio="+dia+"&fim="+dia.plusDays(6),null);
  assertThat(candidatos.size()).isEqualTo(1);assertThat(candidatos.get(0).path("id").asInt()).isEqualTo(origem.getId());
  var previa=ok("/api/v1/remarcacoes/previa",pedido(Modo.MANTER_DATAS));String id=previa.path("operacaoId").asText();
  assertThat(atendimentos.findById(origem.getId()).orElseThrow().getDataHoraInicio()).isEqualTo(dia.atTime(15,0));
  var resultado=ok("/api/v1/remarcacoes/"+id+"/confirmar",Map.of());assertThat(ok("/api/v1/remarcacoes/"+id+"/confirmar",Map.of())).isEqualTo(resultado);
  var atual=atendimentos.findById(origem.getId()).orElseThrow();assertThat(atual.getDataHoraInicio()).isEqualTo(dia.atTime(18,0));assertThat(atual.getDataHoraFim()).isEqualTo(dia.atTime(19,30));assertThat(atual.getValorCobrado()).isEqualByComparingTo("70");assertThat(atual.getPctClinicaSnapshot()).isEqualByComparingTo("25");
  var op=operacoes.findById(id).orElseThrow();assertThat(op.getAutor()).isEqualTo(autor);assertThat(op.getConfirmadaEm()).isNotNull();assertThat(op.getPedido()).contains("Mudança de disponibilidade");assertThat(op.getResultado()).contains("destinoIds");
 }
 @Test void capacidadeAlteradaDepoisDaPreviaExigeNovaConferencia()throws Exception{
  var p=ok("/api/v1/remarcacoes/previa",pedido(Modo.MANTER_DATAS));servico.setCapacidade(2);servicos.save(servico);
  var r=request("/api/v1/remarcacoes/"+p.path("operacaoId").asText()+"/confirmar",Map.of());assertThat(r.getStatusCode().value()).isEqualTo(400);assertThat(r.getBody()).contains("nova prévia");assertThat(atendimentos.findById(origem.getId()).orElseThrow().getDataHoraInicio()).isEqualTo(dia.atTime(15,0));
 }
 @Test void falhaNaCriacaoReverteCancelamentosEAuditoria(){
  var p=service.prever(pedido(Modo.SUBSTITUIR_DIAS),autor);long antes=atendimentos.count();
  doAnswer(i->{Atendimento a=i.getArgument(0);if(a.getId()==null)throw new IllegalStateException("Falha simulada no destino");return i.callRealMethod();}).when(agenda).salvar(any());
  assertThatThrownBy(()->service.confirmar(p.operacaoId(),autor)).isInstanceOf(IllegalStateException.class);
  assertThat(atendimentos.findById(origem.getId()).orElseThrow().getStatus()).isEqualTo(StatusAtendimento.AGENDADO);assertThat(atendimentos.count()).isEqualTo(antes);assertThat(operacoes.findById(p.operacaoId()).orElseThrow().getResultado()).isNull();
 }
 @Test void confirmacoesConcorrentesCriamUmaUnicaProgramacao()throws Exception{
  var p=service.prever(pedido(Modo.SUBSTITUIR_DIAS),autor);long antes=atendimentos.count();
  try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
   var inicio=new CountDownLatch(1);Callable<Resultado> tarefa=()->{inicio.await();return service.confirmar(p.operacaoId(),autor);};
   var a=executor.submit(tarefa);var b=executor.submit(tarefa);inicio.countDown();assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
  }
  assertThat(atendimentos.count()).isEqualTo(antes+2);assertThat(atendimentos.findById(origem.getId()).orElseThrow().getStatus()).isEqualTo(StatusAtendimento.CANCELADO);
 }
 @Test void tokenDeOutroAutorNaoConfirma(){var p=service.prever(pedido(Modo.MANTER_DATAS),autor);assertThatThrownBy(()->service.confirmar(p.operacaoId(),"outro")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThat(atendimentos.findById(origem.getId()).orElseThrow().getDataHoraInicio()).isEqualTo(dia.atTime(15,0));}
}
