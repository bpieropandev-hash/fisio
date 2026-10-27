package com.physio.infrastructure.in.web.controller;
import com.physio.application.service.*;
import com.physio.infrastructure.in.web.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ClinicaControllerTest {
    final FotoClinicaService fotos=mock(FotoClinicaService.class);
    final EvolucaoClinicaService evolucao=mock(EvolucaoClinicaService.class);
    MockMvc mvc() { return MockMvcBuilders.standaloneSetup(new FotoClinicaController(fotos),new EvolucaoClinicaController(evolucao))
            .setControllerAdvice(new ClinicaExceptionHandler(),new GlobalExceptionHandler()).build(); }
    @Test void imagemPrivadaTemTipoCorretoENaoPermiteCache() throws Exception {
        when(fotos.imagem(1,9L)).thenReturn(new FotoClinicaService.Imagem(new byte[]{1,2},"image/png"));
        mvc().perform(get("/api/v1/pacientes/1/fotos/9/imagem"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff"))
                .andExpect(content().bytes(new byte[]{1,2}));
    }
    @Test void conflitoDeVersaoRetorna409() throws Exception {
        when(evolucao.salvar(eq(7),any(),eq("p"))).thenThrow(new EvolucaoVersaoConflitanteException());
        mvc().perform(put("/api/v1/agendamentos/7/evolucao-estruturada").principal(()->"p").contentType("application/json")
                .content("{\"versao\":0,\"procedimentos\":[],\"equipamentos\":[],\"observacoes\":\"\"}"))
                .andExpect(status().isConflict());
    }
    @Test void uploadSemCamposObrigatoriosRetorna400() throws Exception {
        mvc().perform(multipart("/api/v1/pacientes/1/fotos").principal(()->"p"))
                .andExpect(status().isBadRequest());
    }
    @Test void idsInvalidosRetornam400() throws Exception {
        mvc().perform(get("/api/v1/pacientes/abc/fotos")).andExpect(status().isBadRequest());
    }
    @Test void fotoDeOutroPacienteRetorna404SemExporBytes() throws Exception {
        when(fotos.imagem(2,9L)).thenThrow(new jakarta.persistence.EntityNotFoundException("Foto não encontrada."));
        mvc().perform(get("/api/v1/pacientes/2/fotos/9/imagem"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("status").value(404));
    }
    @Test void uploadUsaAutoriaDoPrincipalEDevolve201() throws Exception {
        var arquivo=new org.springframework.mock.web.MockMultipartFile("imagem","f.png","image/png",new byte[]{1});
        mvc().perform(multipart("/api/v1/pacientes/1/fotos").file(arquivo).principal(()->"profissional")
                .param("dataCaptura","2026-10-20").param("regiao","Ombro"))
                .andExpect(status().isCreated());
        verify(fotos).criar(eq(1),any(),eq(java.time.LocalDate.of(2026,10,20)),eq("Ombro"),eq(""),eq("profissional"));
    }
    @Test void exclusaoDevolve204() throws Exception {
        mvc().perform(delete("/api/v1/pacientes/1/fotos/9")).andExpect(status().isNoContent());
        verify(fotos).excluir(1,9L);
    }
}
