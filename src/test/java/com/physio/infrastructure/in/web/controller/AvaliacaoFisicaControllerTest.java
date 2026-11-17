package com.physio.infrastructure.in.web.controller;
import com.physio.application.service.AvaliacaoFisicaService;
import com.physio.infrastructure.in.web.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvaliacaoFisicaControllerTest {
    final AvaliacaoFisicaService service = mock(AvaliacaoFisicaService.class);
    MockMvc mvc() { return MockMvcBuilders.standaloneSetup(new AvaliacaoFisicaController(service))
            .setControllerAdvice(new ClinicaExceptionHandler(), new GlobalExceptionHandler()).build(); }

    @Test void arquivoEntregueComoDownloadIsoladoENaoRenderizadoInline() throws Exception {
        when(service.arquivo(1, 5L)).thenReturn(new AvaliacaoFisicaService.Arquivo(new byte[]{1, 2}, "application/pdf", "ficha inicial.pdf"));
        mvc().perform(get("/api/v1/pacientes/1/avaliacoes/5/arquivo"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(content().bytes(new byte[]{1, 2}));
    }

    @Test void avaliacaoDeOutroPacienteRetorna404() throws Exception {
        when(service.arquivo(2, 5L)).thenThrow(new jakarta.persistence.EntityNotFoundException("Avaliação física não encontrada para este paciente."));
        mvc().perform(get("/api/v1/pacientes/2/avaliacoes/5/arquivo")).andExpect(status().isNotFound());
    }

    @Test void exclusaoDevolve204() throws Exception {
        mvc().perform(delete("/api/v1/pacientes/1/avaliacoes/5")).andExpect(status().isNoContent());
        verify(service).excluir(1, 5L);
    }
}
