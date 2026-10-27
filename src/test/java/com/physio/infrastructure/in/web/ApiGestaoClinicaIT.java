package com.physio.infrastructure.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import com.physio.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises real JWT, controllers, Flyway, PostgreSQL mappings and cross-module contracts. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "api.security.token.secret=integration-only-secret-with-at-least-thirty-two-characters",
        "relatorio.clinica.nome=Clínica de teste",
        "recibo.emitente.nome=Clínica de teste",
        "recibo.emitente.documento=12345678000190",
        "spring.jpa.show-sql=false"
})
@Testcontainers
class ApiGestaoClinicaIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    private static final AtomicInteger sequencia = new AtomicInteger();
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired UsuarioJpaRepository usuarios;
    @Autowired PasswordEncoder passwords;
    private String token;
    private LocalDate dia;

    @BeforeEach
    void autenticar() throws Exception {
        int n = sequencia.incrementAndGet();
        var usuario = new UsuarioEntity();
        usuario.setLogin("integracao" + n);
        usuario.setSenha(passwords.encode("test-password"));
        usuario.setRole("ADMIN");
        usuarios.save(usuario);
        var login = http.postForEntity("/api/auth/login", Map.of("login", usuario.getLogin(), "senha", "test-password"), String.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        token = json.readTree(login.getBody()).path("token").asText();
        assertThat(token).isNotBlank();
        dia = LocalDate.now().plusDays(n + 10L);
    }

    @Test
    void evolucaoPorMarcacaoPersisteResumoHistoricoERejeitaVersaoAntiga() throws Exception {
        int paciente = paciente();
        int servico = servico(2);
        int atendimento = agendar(List.of(paciente), servico, "09:00:00").get(0).path("id").asInt();
        String path = "/api/v1/agendamentos/" + atendimento + "/evolucao-estruturada";
        var ficha = Map.of("versao", 0, "procedimentos", List.of("PRESSAO_ARTERIAL", "PILATES"),
                "equipamentos", List.of("REFORMER"), "pressaoSistolica", 120, "pressaoDiastolica", 80,
                "dor", 3, "observacoes", "Sem intercorrências");
        JsonNode salva = ok(HttpMethod.PUT, path, ficha, 200);
        assertThat(salva.path("versao").asInt()).isEqualTo(1);
        assertThat(salva.path("resumo").asText()).contains("120", "80", "Reformer");
        assertThat(ok(HttpMethod.GET, path, null, 200).path("equipamentos").get(0).asText()).isEqualTo("REFORMER");
        assertThat(ok(HttpMethod.GET, path + "/historico", null, 200).size()).isEqualTo(1);
        assertThat(request(HttpMethod.PUT, path, ficha).getStatusCode().value()).isEqualTo(409);
        assertThat(ok(HttpMethod.GET, "/api/v1/agendamentos/" + atendimento, null, 200).path("evolucao").asText()).isEqualTo(salva.path("resumo").asText());
    }

    @Test
    void fotosPrivadasPersistemBinarioESaoVinculadasAoPaciente() throws Exception {
        int paciente = paciente();
        int outro = paciente();
        String path = "/api/v1/pacientes/" + paciente + "/fotos";
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var form = new LinkedMultiValueMap<String, Object>();
        var imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.IMAGE_PNG);
        form.add("imagem", new HttpEntity<>(new ByteArrayResource(bytes.toByteArray()) {
            @Override public String getFilename() { return "antes.png"; }
        }, imageHeaders));
        form.add("dataCaptura", LocalDate.now().toString());
        form.add("regiao", "Ombro");
        form.add("observacao", "Avaliação inicial");
        var headers = headers(); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var upload = http.exchange(path, HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
        assertThat(upload.getStatusCode().value()).as(upload.getBody()).isEqualTo(201);
        int foto = json.readTree(upload.getBody()).path("id").asInt();
        assertThat(ok(HttpMethod.GET, path, null, 200).size()).isEqualTo(1);
        var imagem = http.exchange(path + "/" + foto + "/imagem", HttpMethod.GET, new HttpEntity<>(headers()), byte[].class);
        assertThat(imagem.getStatusCode().value()).isEqualTo(200);
        assertThat(imagem.getBody()).isNotEmpty();
        assertThat(imagem.getHeaders().getCacheControl()).contains("no-store");
        assertThat(request(HttpMethod.GET, "/api/v1/pacientes/" + outro + "/fotos/" + foto + "/imagem", null).getStatusCode().value()).isEqualTo(404);
        assertThat(http.getForEntity(path + "/" + foto + "/imagem", String.class).getStatusCode().is2xxSuccessful()).isFalse();
        assertThat(request(HttpMethod.DELETE, path + "/" + foto, null).getStatusCode().value()).isEqualTo(204);
        assertThat(ok(HttpMethod.GET, path, null, 200).size()).isZero();
    }

    @Test
    void capacidadeColetivaRespeitaCriacaoERemarcacaoEMensalidadeVinculada() throws Exception {
        int a = paciente(), b = paciente(), c = paciente();
        int servico = servico(2);
        JsonNode turma = agendar(List.of(a, b), servico, "10:00:00");
        assertThat(turma.size()).isEqualTo(2);
        assertThat(turma.get(0).path("dataHoraFim").asText()).isNotBlank();
        assertThat(request(HttpMethod.POST, "/api/v1/agendamentos", agendamento(List.of(c), servico, "10:00:00")).getStatusCode().value()).isEqualTo(400);
        int fora = agendar(List.of(c), servico, "12:00:00").get(0).path("id").asInt();
        assertThat(request(HttpMethod.PUT, "/api/v1/agendamentos/" + fora, Map.of("dataHoraInicio", dia + "T10:00:00")).getStatusCode().value()).isEqualTo(400);
        ok(HttpMethod.POST, "/api/v1/assinaturas", Map.of("pacienteIds", List.of(c), "servicoId", servico,
                "valorMensal", 300, "diaVencimento", 5, "dataInicio", LocalDate.now().toString()), 201);
        var coberto = agendar(List.of(c), servico, "14:00:00").get(0);
        assertThat(coberto.path("modalidadeCobranca").asText()).isEqualTo("MENSALIDADE");
        assertThat(coberto.path("assinaturaId").asInt()).isPositive();
        assertThat(coberto.path("valorCobrado").decimalValue()).isZero();
        assertThat(request(HttpMethod.PUT, "/api/v1/agendamentos/" + coberto.path("id").asInt(),
                Map.of("dataHoraInicio", LocalDate.now().minusDays(1) + "T14:00:00")).getStatusCode().value()).isEqualTo(400);
        assertThat(ok(HttpMethod.GET, "/api/v1/agendamentos/" + coberto.path("id").asInt(), null, 200)
                .path("dataHoraInicio").asText()).isEqualTo(dia + "T14:00:00");
    }

    @Test
    void duasRequisicoesConcorrentesNaoExcedemCapacidade() throws Exception {
        int a = paciente(), b = paciente(), servico = servico(1);
        var iniciar = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> { iniciar.await(); return request(HttpMethod.POST, "/api/v1/agendamentos", agendamento(List.of(a), servico, "08:00:00")).getStatusCode().value(); });
            var segunda = executor.submit(() -> { iniciar.await(); return request(HttpMethod.POST, "/api/v1/agendamentos", agendamento(List.of(b), servico, "08:00:00")).getStatusCode().value(); });
            iniciar.countDown();
            assertThat(List.of(primeira.get(15, java.util.concurrent.TimeUnit.SECONDS), segunda.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 400);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void reciboExigePagamentoEReimpressaoMantemSnapshot() throws Exception {
        int paciente = paciente(), servico = servico(1);
        int atendimento = agendar(List.of(paciente), servico, "16:00:00").get(0).path("id").asInt();
        var pedido = Map.of("atendimentoId", atendimento, "dataPagamento", LocalDate.now().toString(),
                "pagadorNome", "Responsável de teste", "pagadorDocumento", "12345678901");
        assertThat(request(HttpMethod.POST, "/api/v1/recibos", pedido).getStatusCode().value()).isEqualTo(400);
        ok(HttpMethod.PUT, "/api/v1/agendamentos/" + atendimento,
                Map.of("status", "CONCLUIDO", "evolucao", "Sessão realizada", "recebedor", "CLINICA", "tipoPagamento", "PIX", "dataPagamento", LocalDate.now().toString()), 200);
        var emitido = request(HttpMethod.POST, "/api/v1/recibos", pedido);
        assertThat(emitido.getStatusCode().is2xxSuccessful()).as(emitido.getBody()).isTrue();
        JsonNode recibo = json.readTree(emitido.getBody());
        int id = recibo.path("id").asInt();
        assertThat(id).isPositive();
        var repetido = request(HttpMethod.POST, "/api/v1/recibos", pedido);
        assertThat(json.readTree(repetido.getBody()).path("id").asInt()).isEqualTo(id);
        assertThat(ok(HttpMethod.GET, "/api/v1/recibos?atendimentoId=" + atendimento, null, 200).size()).isEqualTo(1);
        var pdf = http.exchange("/api/v1/recibos/" + id + "/pdf", HttpMethod.GET, new HttpEntity<>(headers()), byte[].class);
        assertThat(pdf.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(pdf.getBody(), 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void mensalidadeGeraUmaCobrancaEReciboSomenteDepoisDoRecebimento() throws Exception {
        int paciente = paciente(), servico = servico(1);
        LocalDate hoje = LocalDate.now();
        int assinatura = ok(HttpMethod.POST, "/api/v1/assinaturas", Map.of("pacienteIds", List.of(paciente),
                "servicoId", servico, "valorMensal", 300, "diaVencimento", 5, "dataInicio", hoje.toString()), 201)
                .get(0).path("id").asInt();
        var competencia = Map.of("mes", hoje.getMonthValue(), "ano", hoje.getYear());
        ok(HttpMethod.POST, "/api/v1/financeiro/gerar-mensalidades", competencia, 200);
        ok(HttpMethod.POST, "/api/v1/financeiro/gerar-mensalidades", competencia, 200);
        var cobrancas = ok(HttpMethod.GET, "/api/v1/cobrancas/assinatura/" + assinatura, null, 200);
        assertThat(cobrancas.size()).isEqualTo(1);
        int cobranca = cobrancas.get(0).path("id").asInt();
        var pedido = Map.of("cobrancaId", cobranca, "dataPagamento", hoje.toString(),
                "pagadorNome", "Responsável mensalista", "pagadorDocumento", "12345678901");
        assertThat(request(HttpMethod.POST, "/api/v1/recibos", pedido).getStatusCode().value()).isEqualTo(400);
        ok(HttpMethod.PUT, "/api/v1/cobrancas/" + cobranca, Map.of("status", "PAGO", "dataPagamento", hoje.toString(),
                "recebedor", "CLINICA", "tipoPagamento", "PIX"), 200);
        var resultado = request(HttpMethod.POST, "/api/v1/recibos", pedido);
        assertThat(resultado.getStatusCode().is2xxSuccessful()).as(resultado.getBody()).isTrue();
        var recibo = json.readTree(resultado.getBody());
        assertThat(recibo.path("valor").decimalValue()).isEqualByComparingTo("300");
        assertThat(recibo.path("descricao").asText()).contains("Mensalidade");
        assertThat(ok(HttpMethod.GET, "/api/v1/recibos?cobrancaId=" + cobranca, null, 200).size()).isEqualTo(1);
    }

    private int paciente() throws Exception {
        int n = sequencia.incrementAndGet();
        return ok(HttpMethod.POST, "/api/v1/pacientes", Map.of("nome", "Paciente de teste " + n, "cpf", String.format("%011d", n)), 201).path("id").asInt();
    }
    private int servico(int capacidade) throws Exception {
        return ok(HttpMethod.POST, "/api/v1/servicos", Map.of("nome", "Fisioterapia de teste " + sequencia.incrementAndGet(), "tipo", "FISIOTERAPIA", "valorBase", 100, "pctClinica", 20, "pctProfissional", 80, "capacidade", capacidade, "ativo", true), 201).path("id").asInt();
    }
    private Map<String, Object> agendamento(List<Integer> pacientes, int servico, String hora) {
        return Map.of("pacienteIds", pacientes, "servicoId", servico, "dataHora", dia + "T" + hora);
    }
    private JsonNode agendar(List<Integer> pacientes, int servico, String hora) throws Exception {
        return ok(HttpMethod.POST, "/api/v1/agendamentos", agendamento(pacientes, servico, hora), 201);
    }
    private HttpHeaders headers() { var h = new HttpHeaders(); h.setBearerAuth(token); h.setContentType(MediaType.APPLICATION_JSON); return h; }
    private ResponseEntity<String> request(HttpMethod method, String path, Object body) { return http.exchange(path, method, new HttpEntity<>(body, headers()), String.class); }
    private JsonNode ok(HttpMethod method, String path, Object body, int status) throws Exception {
        var response = request(method, path, body);
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(status);
        return json.readTree(response.getBody());
    }
}
