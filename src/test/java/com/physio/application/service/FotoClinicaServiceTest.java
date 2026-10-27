package com.physio.application.service;
import com.physio.infrastructure.out.persistence.entity.FotoClinicaEntity;
import com.physio.infrastructure.out.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
class FotoClinicaServiceTest {
    final FotoClinicaRepository fotos = mock(FotoClinicaRepository.class);
    final PacienteJpaRepository pacientes = mock(PacienteJpaRepository.class);
    final FotoClinicaService service = new FotoClinicaService(fotos, pacientes);
    final LocalDate data = LocalDate.of(2026, 10, 20);
    @Test void salvaImagemRealComMetadadosEAutoria() throws Exception {
        when(pacientes.existsById(1)).thenReturn(true);
        when(fotos.save(any())).thenAnswer(i -> { FotoClinicaEntity f = i.getArgument(0); f.setId(9L); return f; });
        var result = service.criar(1, imagem(), data, " Coluna ", " Antes ", "profissional");
        assertThat(result.id()).isEqualTo(9L); assertThat(result.pacienteId()).isEqualTo(1);
        assertThat(result.regiao()).isEqualTo("Coluna"); assertThat(result.observacao()).isEqualTo("Antes");
        assertThat(result.autor()).isEqualTo("profissional"); assertThat(result.criadoEm()).isNotNull();
    }
    @Test void naoEntregaOuExcluiImagemDeOutroPaciente() {
        when(fotos.findByIdAndPacienteId(9L, 2)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.imagem(2,9L)).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.excluir(2,9L)).isInstanceOf(EntityNotFoundException.class);
    }
    @Test void rejeitaConteudoFalsoMesmoComMimePng() {
        when(pacientes.existsById(1)).thenReturn(true);
        var f = new MockMultipartFile("imagem", "f.png", "image/png", "falso".getBytes());
        assertThatThrownBy(() -> service.criar(1,f,data,"Coluna","","p")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejeitaTipoMimeDivergenteDoConteudo() throws Exception {
        when(pacientes.existsById(1)).thenReturn(true);
        var f = new MockMultipartFile("imagem", "f.jpg", "image/jpeg", imagem().getBytes());
        assertThatThrownBy(() -> service.criar(1,f,data,"Coluna","","p")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejeitaMaisDeCincoMiB() {
        when(pacientes.existsById(1)).thenReturn(true);
        var f = new MockMultipartFile("imagem", "f.png", "image/png", new byte[5*1024*1024+1]);
        assertThatThrownBy(() -> service.criar(1,f,data,"Coluna","","p")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejeitaDimensoesExcessivasAntesDeDecodificar() throws Exception {
        when(pacientes.existsById(1)).thenReturn(true);
        byte[] png=imagem().getBytes();
        java.nio.ByteBuffer.wrap(png,16,8).putInt(10000).putInt(10000);
        var crc=new java.util.zip.CRC32(); crc.update(png,12,17);
        java.nio.ByteBuffer.wrap(png,29,4).putInt((int)crc.getValue());
        var arquivo=new MockMultipartFile("imagem","grande.png","image/png",png);
        assertThatThrownBy(()->service.criar(1,arquivo,data,"Coluna","","p"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pixels");
        verify(fotos,never()).save(any());
    }
    @Test void rejeitaPacienteInexistente() throws Exception {
        var f=imagem();
        assertThatThrownBy(() -> service.criar(99,f,data,"Coluna","","p")).isInstanceOf(EntityNotFoundException.class);
    }
    @Test void retornaBytesETipoOriginais() throws Exception {
        byte[] bytes=imagem().getBytes();
        when(fotos.findByIdAndPacienteId(9L,1)).thenReturn(Optional.of(FotoClinicaEntity.builder().id(9L).pacienteId(1).conteudo(bytes).tipoConteudo("image/png").build()));
        assertThat(service.imagem(1,9L).conteudo()).isEqualTo(bytes);
        assertThat(service.imagem(1,9L).tipoConteudo()).isEqualTo("image/png");
    }
    private MockMultipartFile imagem() throws Exception {
        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        return new MockMultipartFile("imagem","f.png","image/png",bytes.toByteArray());
    }
}
