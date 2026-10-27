package com.physio.application.service;

import com.physio.domain.model.*;
import com.physio.domain.ports.in.*;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.in.web.controller.CobrancaController;
import com.physio.infrastructure.in.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CobrancaEstornoEndpointTest {
 private final EstornarCobrancaService service=mock(EstornarCobrancaService.class);
 private final MockMvc mvc=MockMvcBuilders.standaloneSetup(new CobrancaController(service,mock(AtualizarCobrancaUseCase.class),mock(ListarCobrancasUseCase.class),mock(CobrancaMensalRepositoryPort.class))).setControllerAdvice(new GlobalExceptionHandler()).build();
 @Test void endpointUsaAutorAutenticadoERetornaPacienteEstruturado() throws Exception {
  var assinatura=Assinatura.builder().id(3).paciente(Paciente.builder().id(7).nome("Ana").build()).servico(ServicoConfig.builder().nome("Pilates").build()).build();
  when(service.estornar(1L,"Correção","profissional")).thenReturn(CobrancaMensal.builder().id(1).assinatura(assinatura).status(StatusCobranca.PENDENTE).build());
  mvc.perform(post("/api/v1/cobrancas/1/estorno").principal(()->"profissional").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Correção\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.pacienteId").value(7)).andExpect(jsonPath("$.pacienteNome").value("Ana")).andExpect(jsonPath("$.descricao").value("Ana - Pilates"));
  verify(service).estornar(1L,"Correção","profissional");
 }
 @Test void reciboEmitidoRetornaErroHttpSemSucessoFalso() throws Exception {
  when(service.estornar(1L,"Correção","profissional")).thenThrow(new IllegalArgumentException("Recibo emitido impede estorno"));
  mvc.perform(post("/api/v1/cobrancas/1/estorno").principal(()->"profissional").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Correção\"}"))
   .andExpect(status().isBadRequest());
 }
 @Test void rejeitaMotivoVazioNoBinding() throws Exception {
  mvc.perform(post("/api/v1/cobrancas/1/estorno").principal(()->"profissional").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\" \"}"))
   .andExpect(status().isBadRequest());
  verifyNoInteractions(service);
 }
}
