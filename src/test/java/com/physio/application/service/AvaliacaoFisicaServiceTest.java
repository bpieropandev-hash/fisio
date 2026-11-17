package com.physio.application.service;

import com.physio.infrastructure.out.persistence.entity.AvaliacaoFisicaEntity;
import com.physio.infrastructure.out.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AvaliacaoFisicaServiceTest {
    final AvaliacaoFisicaRepository avaliacoes = mock(AvaliacaoFisicaRepository.class);
    final PacienteJpaRepository pacientes = mock(PacienteJpaRepository.class);
    final AvaliacaoFisicaService service = new AvaliacaoFisicaService(avaliacoes, pacientes);
    final LocalDate data = LocalDate.of(2026, 11, 17);
    static final byte[] PDF = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

    @Test void salvaPdfValidoComMetadadosENomeSeguro() {
        when(pacientes.existsById(1)).thenReturn(true);
        when(avaliacoes.save(any())).thenAnswer(i -> { AvaliacaoFisicaEntity a = i.getArgument(0); a.setId(5L); return a; });
        var arquivo = new MockMultipartFile("arquivo", "..\\pasta/avaliacao inicial.pdf", "application/pdf", PDF);
        var r = service.criar(1, arquivo, data, " Avaliação inicial ", " Dor lombar ", "profissional");
        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.titulo()).isEqualTo("Avaliação inicial");
        assertThat(r.nomeArquivo()).isEqualTo("avaliacao inicial.pdf");
        assertThat(r.tipoConteudo()).isEqualTo("application/pdf");
        assertThat(r.autor()).isEqualTo("profissional");
    }

    @Test void rejeitaPdfFalsoComMimePdf() {
        when(pacientes.existsById(1)).thenReturn(true);
        var falso = new MockMultipartFile("arquivo", "x.pdf", "application/pdf", "nao sou pdf".getBytes());
        assertThatThrownBy(() -> service.criar(1, falso, data, "Titulo", "", "p"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PDF");
        verify(avaliacoes, never()).save(any());
    }

    @Test void rejeitaTipoNaoPermitido() {
        when(pacientes.existsById(1)).thenReturn(true);
        var exe = new MockMultipartFile("arquivo", "x.html", "text/html", "<script>".getBytes());
        assertThatThrownBy(() -> service.criar(1, exe, data, "Titulo", "", "p")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejeitaMaisDeDezMiB() {
        when(pacientes.existsById(1)).thenReturn(true);
        var grande = new MockMultipartFile("arquivo", "x.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.criar(1, grande, data, "Titulo", "", "p")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejeitaTituloEmBrancoEPacienteInexistente() {
        when(pacientes.existsById(1)).thenReturn(true);
        var arquivo = new MockMultipartFile("arquivo", "x.pdf", "application/pdf", PDF);
        assertThatThrownBy(() -> service.criar(1, arquivo, data, "  ", "", "p")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.criar(99, arquivo, data, "T", "", "p")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test void naoEntregaNemExcluiAvaliacaoDeOutroPaciente() {
        when(avaliacoes.findByIdAndPacienteId(5L, 2)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.arquivo(2, 5L)).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.excluir(2, 5L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test void retornaBytesETipoOriginais() {
        when(avaliacoes.findByIdAndPacienteId(5L, 1)).thenReturn(Optional.of(AvaliacaoFisicaEntity.builder()
                .id(5L).pacienteId(1).conteudo(PDF).tipoConteudo("application/pdf").nomeArquivo("a.pdf").build()));
        var r = service.arquivo(1, 5L);
        assertThat(r.conteudo()).isEqualTo(PDF);
        assertThat(r.tipoConteudo()).isEqualTo("application/pdf");
    }
}
