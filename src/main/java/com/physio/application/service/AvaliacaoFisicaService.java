package com.physio.application.service;

import com.physio.infrastructure.in.web.dto.AvaliacaoFisicaDTO;
import com.physio.infrastructure.out.persistence.entity.AvaliacaoFisicaEntity;
import com.physio.infrastructure.out.persistence.repository.AvaliacaoFisicaRepository;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Avaliação física do paciente: imagem (JPEG/PNG) ou PDF, separada das fotos de evolução. */
@Service
@RequiredArgsConstructor
public class AvaliacaoFisicaService {
    static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000;
    private static final String MSG_ARQUIVO = "Envie uma imagem JPEG, PNG ou um PDF de até 10 MiB.";
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "application/pdf");
    private final AvaliacaoFisicaRepository avaliacoes;
    private final PacienteJpaRepository pacientes;

    public record Arquivo(byte[] conteudo, String tipoConteudo, String nomeArquivo) {}

    @Transactional(readOnly = true)
    public List<AvaliacaoFisicaDTO> listar(Integer pacienteId) {
        exigirPaciente(pacienteId);
        return avaliacoes.listarMetadados(pacienteId);
    }

    @Transactional
    public AvaliacaoFisicaDTO criar(Integer pacienteId, MultipartFile arquivo, LocalDate data,
                                    String titulo, String observacao, String autor) {
        exigirPaciente(pacienteId);
        if (data == null) throw new IllegalArgumentException("Informe a data da avaliação.");
        titulo = texto(titulo, 120, true, "título");
        observacao = texto(observacao, 2000, false, "observação");
        autor = texto(autor, 255, true, "autor");
        byte[] bytes = validarArquivo(arquivo);
        var salva = avaliacoes.save(AvaliacaoFisicaEntity.builder().pacienteId(pacienteId).dataAvaliacao(data)
                .titulo(titulo).observacao(observacao).autor(autor).criadoEm(LocalDateTime.now())
                .nomeArquivo(nomeSeguro(arquivo.getOriginalFilename())).conteudo(bytes)
                .tipoConteudo(arquivo.getContentType()).build());
        return new AvaliacaoFisicaDTO(salva.getId(), salva.getPacienteId(), salva.getDataAvaliacao(), salva.getTitulo(),
                salva.getObservacao(), salva.getCriadoEm(), salva.getAutor(), salva.getNomeArquivo(), salva.getTipoConteudo());
    }

    @Transactional(readOnly = true)
    public Arquivo arquivo(Integer pacienteId, Long id) {
        var a = buscar(pacienteId, id);
        return new Arquivo(a.getConteudo(), a.getTipoConteudo(), a.getNomeArquivo());
    }

    @Transactional
    public void excluir(Integer pacienteId, Long id) {
        avaliacoes.delete(buscar(pacienteId, id));
    }

    private AvaliacaoFisicaEntity buscar(Integer pacienteId, Long id) {
        return avaliacoes.findByIdAndPacienteId(id, pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Avaliação física não encontrada para este paciente."));
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

    /** Só o nome base, sem caminho nem caracteres de controle, limitado ao tamanho da coluna. */
    private String nomeSeguro(String original) {
        String nome = original == null ? "" : original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (nome.isEmpty()) nome = "avaliacao";
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }

    private byte[] validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty() || arquivo.getSize() > MAX_BYTES) throw new IllegalArgumentException(MSG_ARQUIVO);
        String tipo = arquivo.getContentType();
        if (tipo == null || !TIPOS.contains(tipo)) throw new IllegalArgumentException(MSG_ARQUIVO);
        try {
            byte[] bytes;
            try (var stream = arquivo.getInputStream()) {
                bytes = stream.readNBytes((int) MAX_BYTES + 1);
            }
            if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IllegalArgumentException(MSG_ARQUIVO);
            if ("application/pdf".equals(tipo)) validarPdf(bytes); else validarImagem(bytes, tipo);
            return bytes;
        } catch (IOException | IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Arquivo inválido ou corrompido.", ex);
        }
    }

    private void validarPdf(byte[] bytes) {
        // Cabeçalho %PDF- nos primeiros 1024 bytes (a especificação tolera lixo antes do cabeçalho).
        String inicio = new String(bytes, 0, Math.min(bytes.length, 1024), StandardCharsets.ISO_8859_1);
        if (!inicio.contains("%PDF-")) throw new IllegalArgumentException("O arquivo não é um PDF válido.");
    }

    private void validarImagem(byte[] bytes, String tipo) throws IOException {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("O arquivo não contém uma imagem válida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String esperado = "image/png".equals(tipo) ? "png" : "JPEG";
                if (!esperado.equalsIgnoreCase(reader.getFormatName()))
                    throw new IllegalArgumentException("O conteúdo não corresponde ao tipo de arquivo informado.");
                int largura = reader.getWidth(0), altura = reader.getHeight(0);
                if (largura <= 0 || altura <= 0 || (long) largura * altura > MAX_PIXELS)
                    throw new IllegalArgumentException("A imagem deve ter no máximo 20 milhões de pixels.");
                if (reader.read(0) == null) throw new IllegalArgumentException("Não foi possível decodificar a imagem.");
            } finally {
                reader.dispose();
            }
        }
    }
}
