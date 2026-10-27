package com.physio.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.physio.domain.model.Atendimento;
import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.Paciente;
import com.physio.domain.model.Recebedor;
import com.physio.domain.model.StatusAtendimento;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.domain.ports.out.PacienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioPDFService {

    private static final Color COR_CABECALHO_TABELA = new Color(0x3f, 0x51, 0x5c);
    private static final Color COR_ZEBRA = new Color(0xf2, 0xf5, 0xf7);
    private static final Color COR_DIVISOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Color COR_DESTAQUE = new Color(0xe8, 0xf0, 0xfe);
    private static final Color COR_NEGATIVO = new Color(0xc0, 0x39, 0x2b);
    private static final Color COR_POSITIVO = new Color(0x1e, 0x7a, 0x34);

    private final AtendimentoRepositoryPort atendimentoRepositoryPort;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;

    @Value("${relatorio.clinica.nome}")
    private String nomeClinica;

    @Value("${relatorio.profissional.nome}")
    private String nomeProfissional;

    @Value("${relatorio.profissional.crefito}")
    private String crefitoProfissional;

    public byte[] gerarRelatorioPersonalizado(LocalDateTime inicio, LocalDateTime fim, List<Integer> servicoIds) {
        List<Atendimento> atendimentos = atendimentoRepositoryPort.listarParaRelatorioFinanceiro(inicio, fim, servicoIds)
                .stream()
                .filter(a -> a.getValorCobrado().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        int mesInicio = inicio.getMonthValue();
        int anoInicio = inicio.getYear();
        int mesFim = fim.getMonthValue();
        int anoFim = fim.getYear();

        List<CobrancaMensal> cobrancasMensais = cobrancaMensalRepositoryPort.buscarPagasPorPeriodo(
                anoInicio, mesInicio, anoFim, mesFim
        );

        if (servicoIds != null && !servicoIds.isEmpty()) {
            cobrancasMensais = cobrancasMensais.stream()
                    .filter(c -> servicoIds.contains(c.getAssinatura().getServico().getId()))
                    .toList();
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        byte[] rascunho = construirAcertoFinanceiro(atendimentos, cobrancasMensais, inicio, fim, fmtData, null);
        int totalPaginas = contarPaginas(rascunho);
        return construirAcertoFinanceiro(atendimentos, cobrancasMensais, inicio, fim, fmtData, totalPaginas);
    }

    private byte[] construirAcertoFinanceiro(List<Atendimento> atendimentos, List<CobrancaMensal> cobrancasMensais,
                                              LocalDateTime inicio, LocalDateTime fim, DateTimeFormatter fmtData,
                                              Integer totalPaginas) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new RodapePaginacaoEvent(totalPaginas));
            document.open();

            adicionarCabecalho(document, "Acerto Financeiro",
                    "Período: " + inicio.format(fmtData) + " até " + fim.format(fmtData));

            PdfPTable table = new PdfPTable(new float[]{2f, 4f, 2.5f, 2f, 2.5f, 4f, 3f});
            table.setWidthPercentage(100);

            addHeaderCell(table, "Data");
            addHeaderCell(table, "Paciente");
            addHeaderCell(table, "Serviço");
            addHeaderCell(table, "% Clínica");
            addHeaderCell(table, "% Profissional");
            addHeaderCell(table, "Forma Pagto.");
            addHeaderCell(table, "Acerto");

            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
            DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("dd/MM HH:mm");

            BigDecimal saldoFinalProfissional = BigDecimal.ZERO;
            BigDecimal totalParteClinica = BigDecimal.ZERO;
            BigDecimal totalParteProfissional = BigDecimal.ZERO;
            BigDecimal totalAvulsos = BigDecimal.ZERO;
            BigDecimal totalMensalidades = BigDecimal.ZERO;

            List<ItemFinanceiro> itensFinanceiros = new ArrayList<>();

            for (Atendimento a : atendimentos) {
                itensFinanceiros.add(new ItemFinanceiro(
                        a.getDataHoraInicio().toLocalDate(),
                        a.getPaciente().getNome(),
                        a.getServicoBase().getNome(),
                        a.getValorCobrado(),
                        a.getTipoPagamento() != null ? a.getTipoPagamento().toString() : "",
                        a.getRecebedor(),
                        a.getPctClinicaSnapshot(),
                        a.getPctProfissionalSnapshot(),
                        a.getDataHoraInicio(),
                        true
                ));
            }

            for (CobrancaMensal c : cobrancasMensais) {
                LocalDate dataReferencia = LocalDate.of(c.getAnoReferencia(), c.getMesReferencia(), 1);
                LocalDate dataExibicao = c.getDataPagamento() != null ? c.getDataPagamento() : dataReferencia;

                itensFinanceiros.add(new ItemFinanceiro(
                        dataExibicao,
                        c.getAssinatura().getPaciente().getNome(),
                        c.getAssinatura().getServico().getNome() + " (Mensalidade)",
                        c.getValor(),
                        c.getTipoPagamento() != null ? c.getTipoPagamento().toString() : "",
                        c.getRecebedor(),
                        c.getPctClinicaSnapshot(),
                        c.getPctProfissionalSnapshot(),
                        null,
                        false
                ));
            }

            itensFinanceiros.sort(Comparator.comparing(ItemFinanceiro::getData));

            boolean linhaPar = false;
            for (ItemFinanceiro item : itensFinanceiros) {
                Color corLinha = linhaPar ? COR_ZEBRA : Color.WHITE;
                linhaPar = !linhaPar;

                String dataFormatada = item.isAtendimento() && item.getDataHora() != null
                        ? item.getDataHora().format(fmtHora)
                        : item.getData().format(fmtData);
                addCell(table, dataFormatada, Element.ALIGN_LEFT, corLinha);
                addCell(table, item.getPaciente(), Element.ALIGN_LEFT, corLinha);
                addCell(table, item.getServico(), Element.ALIGN_LEFT, corLinha);
                addCell(table, formatPercent(item.getPctClinica()), Element.ALIGN_CENTER, corLinha);
                addCell(table, formatPercent(item.getPctProfissional()), Element.ALIGN_CENTER, corLinha);

                String infoPagto = nf.format(item.getValor()) + "\n" +
                        item.getTipoPagamento() + " " +
                        (item.getRecebedor() != null ? item.getRecebedor().toString() : "");
                addCell(table, infoPagto, Element.ALIGN_RIGHT, corLinha);

                BigDecimal valorTotal = item.getValor();
                BigDecimal parteProf = valorTotal.multiply(item.getPctProfissional()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                BigDecimal parteClinica = valorTotal.multiply(item.getPctClinica()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                totalParteClinica = totalParteClinica.add(parteClinica);
                totalParteProfissional = totalParteProfissional.add(parteProf);
                if (item.isAtendimento()) {
                    totalAvulsos = totalAvulsos.add(valorTotal);
                } else {
                    totalMensalidades = totalMensalidades.add(valorTotal);
                }

                if (item.getRecebedor() == Recebedor.CLINICA) {
                    BigDecimal valorAcerto = parteProf;
                    saldoFinalProfissional = saldoFinalProfissional.add(valorAcerto);
                    addCellColorida(table, "+ " + nf.format(valorAcerto), Element.ALIGN_RIGHT, corLinha, COR_POSITIVO);
                } else if (item.getRecebedor() == Recebedor.PROFISSIONAL) {
                    BigDecimal valorAcerto = parteClinica.negate();
                    saldoFinalProfissional = saldoFinalProfissional.add(valorAcerto);
                    addCellColorida(table, "- " + nf.format(parteClinica), Element.ALIGN_RIGHT, corLinha, COR_NEGATIVO);
                } else {
                    addCell(table, "Pend.", Element.ALIGN_RIGHT, corLinha);
                }
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            document.add(adicionarResumoFinanceiro(nf, totalAvulsos, totalMensalidades,
                    totalParteClinica, totalParteProfissional, saldoFinalProfissional));

            Font fontLegenda = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
            document.add(new Paragraph("\n* Valores positivos: Clínica recebeu e deve repassar comissão.", fontLegenda));
            document.add(new Paragraph("* Valores negativos: Profissional recebeu integral e deve devolver parte da clínica (descontado do saldo).", fontLegenda));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório", e);
        }
    }

    public byte[] gerarProntuario(Long pacienteId) {
        Paciente paciente = pacienteRepositoryPort.buscarPorId(pacienteId)
                .orElse(null);

        List<Atendimento> atendimentos = atendimentoRepositoryPort.listarPorPaciente(pacienteId).stream()
                .filter(a -> a.getStatus() == StatusAtendimento.CONCLUIDO)
                .sorted(Comparator.comparing(Atendimento::getDataHoraInicio))
                .collect(Collectors.toList());

        byte[] rascunho = construirProntuario(paciente, atendimentos, null);
        int totalPaginas = contarPaginas(rascunho);
        return construirProntuario(paciente, atendimentos, totalPaginas);
    }

    private byte[] construirProntuario(Paciente paciente, List<Atendimento> atendimentos, Integer totalPaginas) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new RodapePaginacaoEvent(totalPaginas));
            document.open();

            adicionarCabecalho(document, "Prontuário", null);
            document.add(criarBlocoPaciente(paciente));
            document.add(Chunk.NEWLINE);

            if (atendimentos.isEmpty()) {
                Paragraph vazio = new Paragraph("Nenhum atendimento concluído encontrado.",
                        FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY));
                vazio.setAlignment(Element.ALIGN_CENTER);
                document.add(vazio);
                document.close();
                return out.toByteArray();
            }

            DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Font fontDataServico = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font fontRotuloEvolucao = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.GRAY);
            Font fontCorpo = FontFactory.getFont(FontFactory.HELVETICA, 11);

            for (Atendimento a : atendimentos) {
                PdfPTable card = new PdfPTable(1);
                card.setWidthPercentage(100);
                card.setSpacingBefore(10);
                card.setKeepTogether(true);

                PdfPCell cabecalhoCard = new PdfPCell(new Phrase(
                        a.getDataHoraInicio().format(fmtHora) + "  •  " +
                                (a.getServicoBase() != null ? a.getServicoBase().getNome() : "-"),
                        fontDataServico));
                cabecalhoCard.setBackgroundColor(COR_ZEBRA);
                cabecalhoCard.setPadding(8);
                cabecalhoCard.setBorderColor(COR_DIVISOR);
                card.addCell(cabecalhoCard);

                Paragraph corpo = new Paragraph();
                corpo.add(new Phrase("Evolução: ", fontRotuloEvolucao));
                corpo.add(new Phrase(a.getEvolucao() != null ? a.getEvolucao() : "--", fontCorpo));

                PdfPCell corpoCard = new PdfPCell(corpo);
                corpoCard.setPadding(10);
                corpoCard.setBorderColor(COR_DIVISOR);
                card.addCell(corpoCard);

                document.add(card);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar prontuário", e);
        }
    }

    private void adicionarCabecalho(Document document, String tituloRelatorio, String subtitulo) throws DocumentException {
        Font fontClinica = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COR_CABECALHO_TABELA);
        Font fontProfissional = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

        Paragraph clinica = new Paragraph(nomeClinica, fontClinica);
        clinica.setAlignment(Element.ALIGN_CENTER);
        document.add(clinica);

        if (nomeProfissional != null && !nomeProfissional.isBlank()) {
            String linhaProfissional = crefitoProfissional != null && !crefitoProfissional.isBlank()
                    ? nomeProfissional + " — CREFITO " + crefitoProfissional
                    : nomeProfissional;
            Paragraph profissional = new Paragraph(linhaProfissional, fontProfissional);
            profissional.setAlignment(Element.ALIGN_CENTER);
            document.add(profissional);
        }

        document.add(Chunk.NEWLINE);
        document.add(criarLinhaDivisoria());
        document.add(Chunk.NEWLINE);

        Paragraph titulo = new Paragraph(tituloRelatorio, fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        if (subtitulo != null) {
            Paragraph sub = new Paragraph(subtitulo, fontSubtitulo);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingBefore(2);
            document.add(sub);
        }

        Paragraph emissao = new Paragraph(
                "Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                fontSubtitulo);
        emissao.setAlignment(Element.ALIGN_RIGHT);
        emissao.setSpacingBefore(4);
        document.add(emissao);
        document.add(Chunk.NEWLINE);
    }

    private PdfPTable criarLinhaDivisoria() {
        PdfPTable divisor = new PdfPTable(1);
        divisor.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(1.5f);
        cell.setBackgroundColor(COR_DIVISOR);
        cell.setBorder(Rectangle.NO_BORDER);
        divisor.addCell(cell);
        return divisor;
    }

    private PdfPTable criarBlocoPaciente(Paciente paciente) {
        PdfPTable bloco = new PdfPTable(2);
        bloco.setWidthPercentage(100);
        bloco.setWidths(new float[]{1f, 1f});

        Font fontRotulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.GRAY);
        Font fontValor = FontFactory.getFont(FontFactory.HELVETICA, 12);

        String nome = paciente != null ? paciente.getNome() : "-";
        String dataNasc = paciente != null && paciente.getDataNascimento() != null
                ? paciente.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
        String cpf = paciente != null && paciente.getCpf() != null ? paciente.getCpf() : "-";
        String telefone = paciente != null && paciente.getTelefone() != null ? paciente.getTelefone() : "-";

        adicionarCampoBloco(bloco, "Paciente", nome, fontRotulo, fontValor);
        adicionarCampoBloco(bloco, "Data de Nascimento", dataNasc, fontRotulo, fontValor);
        adicionarCampoBloco(bloco, "CPF", cpf, fontRotulo, fontValor);
        adicionarCampoBloco(bloco, "Telefone", telefone, fontRotulo, fontValor);

        return bloco;
    }

    private void adicionarCampoBloco(PdfPTable bloco, String rotulo, String valor, Font fontRotulo, Font fontValor) {
        Paragraph conteudo = new Paragraph();
        conteudo.add(new Phrase(rotulo.toUpperCase() + "\n", fontRotulo));
        conteudo.add(new Phrase(valor, fontValor));

        PdfPCell cell = new PdfPCell(conteudo);
        cell.setBackgroundColor(COR_DESTAQUE);
        cell.setPadding(10);
        cell.setBorderColor(COR_DIVISOR);
        bloco.addCell(cell);
    }

    private PdfPTable adicionarResumoFinanceiro(NumberFormat nf, BigDecimal totalAvulsos, BigDecimal totalMensalidades,
                                                 BigDecimal totalParteClinica, BigDecimal totalParteProfissional,
                                                 BigDecimal saldoFinalProfissional) {
        PdfPTable resumo = new PdfPTable(2);
        resumo.setWidthPercentage(60);
        resumo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        resumo.setWidths(new float[]{3f, 2f});

        addHeaderCell(resumo, "Resumo Financeiro");
        addHeaderCell(resumo, "Valor");

        addCell(resumo, "Subtotal Atendimentos Avulsos", Element.ALIGN_LEFT, Color.WHITE);
        addCell(resumo, nf.format(totalAvulsos), Element.ALIGN_RIGHT, Color.WHITE);

        addCell(resumo, "Subtotal Mensalidades", Element.ALIGN_LEFT, COR_ZEBRA);
        addCell(resumo, nf.format(totalMensalidades), Element.ALIGN_RIGHT, COR_ZEBRA);

        addCell(resumo, "Total Parte Clínica", Element.ALIGN_LEFT, Color.WHITE);
        addCell(resumo, nf.format(totalParteClinica), Element.ALIGN_RIGHT, Color.WHITE);

        addCell(resumo, "Total Parte Profissional", Element.ALIGN_LEFT, COR_ZEBRA);
        addCell(resumo, nf.format(totalParteProfissional), Element.ALIGN_RIGHT, COR_ZEBRA);

        Font fontDestaqueRotulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font fontDestaqueValor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COR_CABECALHO_TABELA);

        PdfPCell rotuloSaldo = new PdfPCell(new Phrase("Saldo a Repassar para Profissional", fontDestaqueRotulo));
        rotuloSaldo.setBackgroundColor(COR_DESTAQUE);
        rotuloSaldo.setPadding(8);
        resumo.addCell(rotuloSaldo);

        PdfPCell valorSaldo = new PdfPCell(new Phrase(nf.format(saldoFinalProfissional), fontDestaqueValor));
        valorSaldo.setBackgroundColor(COR_DESTAQUE);
        valorSaldo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valorSaldo.setPadding(8);
        resumo.addCell(valorSaldo);

        return resumo;
    }

    /**
     * Conta páginas reais de um PDF já gerado. Usado no padrão "gera rascunho, conta, gera de novo com
     * o total certo" — mais simples e confiável que o truque clássico de PdfTemplate no rodapé, que
     * no OpenPDF pode reportar 1 página a mais do que o PDF realmente tem.
     */
    private int contarPaginas(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            int paginas = reader.getNumberOfPages();
            reader.close();
            return paginas;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar páginas do PDF", e);
        }
    }

    private String formatPercent(BigDecimal pct) {
        if (pct == null) return "";
        try {
            return pct.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
        } catch (Exception e) {
            return pct.toString() + "%";
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setBackgroundColor(COR_CABECALHO_TABELA);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, int alinhamento, Color corFundo) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alinhamento);
        cell.setBackgroundColor(corFundo);
        cell.setBorderColor(COR_DIVISOR);
        table.addCell(cell);
    }

    private void addCellColorida(PdfPTable table, String text, int alinhamento, Color corFundo, Color corTexto) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, corTexto)));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alinhamento);
        cell.setBackgroundColor(corFundo);
        cell.setBorderColor(COR_DIVISOR);
        table.addCell(cell);
    }

    /**
     * Rodapé "Página X de Y". O total já vem pronto (calculado num rascunho anterior via
     * {@link #contarPaginas(byte[])}) — evita o truque clássico de PdfTemplate, que no OpenPDF
     * pode contar 1 página fantasma a mais.
     */
    private static class RodapePaginacaoEvent extends PdfPageEventHelper {
        private final Integer totalPaginas;
        private BaseFont baseFont;

        RodapePaginacaoEvent(Integer totalPaginas) {
            this.totalPaginas = totalPaginas;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao carregar fonte do rodapé", e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            String texto = totalPaginas != null
                    ? "Página " + writer.getPageNumber() + " de " + totalPaginas
                    : "Página " + writer.getPageNumber();
            float largura = baseFont.getWidthPoint(texto, 8);

            cb.saveState();
            cb.beginText();
            cb.setFontAndSize(baseFont, 8);
            cb.setTextMatrix(document.right() - largura, document.bottom() - 20);
            cb.showText(texto);
            cb.endText();
            cb.restoreState();
        }
    }

    private static class ItemFinanceiro {
        private final LocalDate data;
        private final String paciente;
        private final String servico;
        private final BigDecimal valor;
        private final String tipoPagamento;
        private final Recebedor recebedor;
        private final BigDecimal pctClinica;
        private final BigDecimal pctProfissional;
        private final LocalDateTime dataHora;
        private final boolean isAtendimento;

        ItemFinanceiro(LocalDate data, String paciente, String servico, BigDecimal valor,
                       String tipoPagamento, Recebedor recebedor, BigDecimal pctClinica,
                       BigDecimal pctProfissional, LocalDateTime dataHora, boolean isAtendimento) {
            this.data = data;
            this.paciente = paciente;
            this.servico = servico;
            this.valor = valor;
            this.tipoPagamento = tipoPagamento;
            this.recebedor = recebedor;
            this.pctClinica = pctClinica;
            this.pctProfissional = pctProfissional;
            this.dataHora = dataHora;
            this.isAtendimento = isAtendimento;
        }

        LocalDate getData() { return data; }
        String getPaciente() { return paciente; }
        String getServico() { return servico; }
        BigDecimal getValor() { return valor; }
        String getTipoPagamento() { return tipoPagamento; }
        Recebedor getRecebedor() { return recebedor; }
        BigDecimal getPctClinica() { return pctClinica; }
        BigDecimal getPctProfissional() { return pctProfissional; }
        LocalDateTime getDataHora() { return dataHora; }
        boolean isAtendimento() { return isAtendimento; }
    }
}
