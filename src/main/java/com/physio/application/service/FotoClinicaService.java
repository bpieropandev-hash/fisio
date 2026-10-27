package com.physio.application.service;

import com.physio.infrastructure.in.web.dto.FotoClinicaDTO;
import com.physio.infrastructure.out.persistence.entity.FotoClinicaEntity;
import com.physio.infrastructure.out.persistence.repository.FotoClinicaRepository;
import com.physio.infrastructure.out.persistence.repository.PacienteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FotoClinicaService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000;
    private final FotoClinicaRepository fotos;
    private final PacienteJpaRepository pacientes;
    private final ConfiguracoesService configuracoes;

    public record Imagem(byte[] conteudo, String tipoConteudo) {}

    @Transactional(readOnly = true)
    public List<FotoClinicaDTO> listar(Integer pacienteId) {
        exigirPaciente(pacienteId);
        return fotos.listarMetadados(pacienteId);
    }

    @Transactional
    public FotoClinicaDTO criar(Integer pacienteId, MultipartFile imagem, LocalDate data,
                                String regiao, String observacao, String autor) {
        exigirPaciente(pacienteId);
        if (data == null) throw new IllegalArgumentException("Informe a data da captura.");
        regiao = texto(regiao, 100, true, "região");
        observacao = texto(observacao, 2000, false, "observação");
        autor = texto(autor, 255, true, "autor");
        byte[] bytes = validarImagem(imagem);
        var foto = fotos.save(FotoClinicaEntity.builder().pacienteId(pacienteId).dataCaptura(data)
                .regiao(regiao).observacao(observacao).autor(autor).criadoEm(LocalDateTime.now())
                .conteudo(bytes).tipoConteudo(imagem.getContentType()).build());
        return new FotoClinicaDTO(foto.getId(), foto.getPacienteId(), foto.getDataCaptura(),
                foto.getRegiao(), foto.getObservacao(), foto.getCriadoEm(), foto.getAutor());
    }

    @Transactional(readOnly = true)
    public Imagem imagem(Integer pacienteId, Long id) {
        var foto = buscar(pacienteId, id);
        return new Imagem(foto.getConteudo(), foto.getTipoConteudo());
    }

    @Transactional
    public void excluir(Integer pacienteId, Long id) {
        fotos.delete(buscar(pacienteId, id));
    }

    private FotoClinicaEntity buscar(Integer pacienteId, Long id) {
        return fotos.findByIdAndPacienteId(id, pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Foto clínica não encontrada para este paciente."));
    }

    private void exigirPaciente(Integer id) {
        if (id == null || !pacientes.existsById(id)) throw new EntityNotFoundException("Paciente não encontrado.");
    }

    private String texto(String valor, int limite, boolean obrigatorio, String nome) {
        if (valor == null) valor = "";
        if (valor.length() > limite || (obrigatorio && valor.isBlank()))
            throw new IllegalArgumentException("Campo " + nome + " inválido (máximo " + limite + " caracteres).");
        return valor.trim();
    }

    private byte[] validarImagem(MultipartFile imagem) {
        int limiteMiB=configuracoes.buscar().fotoLimiteMiB();
        long limite=Math.min(MAX_BYTES, limiteMiB*1024L*1024L);
        if (imagem == null || imagem.isEmpty() || imagem.getSize() > limite)
            throw new IllegalArgumentException("Envie uma imagem JPEG ou PNG de até "+limiteMiB+" MiB.");
        if (imagem.getContentType() == null || !Set.of("image/jpeg", "image/png").contains(imagem.getContentType()))
            throw new IllegalArgumentException("Apenas imagens JPEG e PNG são aceitas.");
        try {
            byte[] bytes;
            try (var stream = imagem.getInputStream()) {
                bytes = stream.readNBytes((int) limite + 1);
            }
            if (bytes.length == 0 || bytes.length > limite) throw new IllegalArgumentException("Imagem excede "+limiteMiB+" MiB.");
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("O arquivo não contém uma imagem válida.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String formato = reader.getFormatName();
                    String esperado = "image/png".equals(imagem.getContentType()) ? "png" : "JPEG";
                    if (!esperado.equalsIgnoreCase(formato)) throw new IllegalArgumentException("O conteúdo não corresponde ao tipo de imagem informado.");
                    int largura = reader.getWidth(0), altura = reader.getHeight(0);
                    if (largura <= 0 || altura <= 0 || (long) largura * altura > MAX_PIXELS)
                        throw new IllegalArgumentException("A imagem deve ter no máximo 20 milhões de pixels.");
                    if (reader.read(0) == null) throw new IllegalArgumentException("Não foi possível decodificar a imagem.");
                } finally {
                    reader.dispose();
                }
            }
            return bytes;
        } catch (IOException | IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Imagem inválida ou corrompida.", ex);
        }
    }
}
