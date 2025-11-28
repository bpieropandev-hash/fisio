package com.physio.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.physio.domain.model.Recebedor;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RelatorioPDFService {

    private final AtendimentoJpaRepository repository;

    public byte[] gerarRelatorioPersonalizado(LocalDateTime inicio, LocalDateTime fim, List<Integer> servicoIds) {
        // 1. Busca os dados
        List<AtendimentoEntity> atendimentos = repository.findParaRelatorioFinanceiro(inicio, fim, servicoIds);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4); // Layout retrato padrão
            PdfWriter.getInstance(document, out);
            document.open();

            // --- TÍTULO ---
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            Paragraph titulo = new Paragraph("Relatório de Atendimentos", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subTitulo = new Paragraph("Período: " + inicio.format(fmtData) + " até " + fim.format(fmtData), FontFactory.getFont(FontFactory.HELVETICA, 12));
            subTitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(subTitulo);
            document.add(Chunk.NEWLINE);

            // --- TABELA ---
            // Colunas: Data | Paciente | Serviço | Pagamento (Info) | Acerto (Valor)
            PdfPTable table = new PdfPTable(new float[]{ 2f, 4f, 3f, 4f, 3f });
            table.setWidthPercentage(100);

            // Cabeçalho estilizado
            addHeaderCell(table, "Data");
            addHeaderCell(table, "Paciente");
            addHeaderCell(table, "Serviço");
            addHeaderCell(table, "Forma Pagto.");
            addHeaderCell(table, "Crédito/Débito"); // O que entra/sai para a profissional

            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("dd/MM HH:mm");

            BigDecimal saldoFinalProfissional = BigDecimal.ZERO;

            for (AtendimentoEntity a : atendimentos) {
                // Preencher Colunas Básicas
                addCell(table, a.getDataHoraInicio().format(fmtHora));
                addCell(table, a.getPaciente().getNome()); // Supondo getPaciente().getNome()
                addCell(table, a.getServicoBase().getNome());

                // Lógica de Exibição do Pagamento (Ex: "R$ 170,00 Pix Clinica")
                String infoPagto = nf.format(a.getValorCobrado()) + "\n" +
                        (a.getTipoPagamento() != null ? a.getTipoPagamento() : "") + " " +
                        (a.getRecebedor() != null ? a.getRecebedor().toString() : "");
                addCell(table, infoPagto);

                // --- A LÓGICA DO ACERTO FINANCEIRO ---
                BigDecimal valorAcerto = BigDecimal.ZERO;

                // Cálculo das partes
                BigDecimal valorTotal = a.getValorCobrado();
                BigDecimal parteProf = valorTotal.multiply(a.getPctProfissionalSnapshot()).divide(new BigDecimal(100));
                BigDecimal parteClinica = valorTotal.multiply(a.getPctClinicaSnapshot()).divide(new BigDecimal(100));

                if (a.getRecebedor() == Recebedor.CLINICA) {
                    // Clínica recebeu tudo. Clínica deve pagar a parte da profissional.
                    // EFEITO: + Crédito para a profissional
                    valorAcerto = parteProf;
                    saldoFinalProfissional = saldoFinalProfissional.add(valorAcerto);

                    // Visual na tabela (Positivo em azul ou preto)
                    addCell(table, "+ " + nf.format(valorAcerto));

                } else if (a.getRecebedor() == Recebedor.PROFISSIONAL) {
                    // Profissional recebeu tudo (100%).
                    // Mas ela só é dona da 'parteProf'. Ela está segurando a 'parteClinica'.
                    // No acerto, devemos descontar a parte da clínica do que ela tem a receber de outros atendimentos.
                    // EFEITO: - Débito para a profissional
                    valorAcerto = parteClinica.negate(); // Negativo
                    saldoFinalProfissional = saldoFinalProfissional.add(valorAcerto);

                    // Visual na tabela (Negativo em vermelho)
                    PdfPCell cellNeg = new PdfPCell(new Phrase("- " + nf.format(parteClinica), FontFactory.getFont(FontFactory.HELVETICA, 10, Color.RED)));
                    cellNeg.setPadding(5);
                    table.addCell(cellNeg);
                } else {
                    addCell(table, "Pend.");
                }
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // --- RESUMO FINAL ---
            PdfPTable resumoTable = new PdfPTable(2);
            resumoTable.setWidthPercentage(50);
            resumoTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addHeaderCell(resumoTable, "Resumo do Acerto");
            addHeaderCell(resumoTable, "Valor");

            addCell(resumoTable, "A Repassar para Profissional:");
            PdfPCell cellTotal = new PdfPCell(new Phrase(nf.format(saldoFinalProfissional), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            resumoTable.addCell(cellTotal);

            document.add(resumoTable);

            // Explicação da lógica no rodapé para não ter confusão
            Font fontLegenda = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);
            document.add(new Paragraph("\n* Valores positivos: Clínica recebeu e deve repassar comissão.", fontLegenda));
            document.add(new Paragraph("* Valores negativos: Profissional recebeu integral e deve devolver parte da clínica (descontado do saldo).", fontLegenda));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório", e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setPadding(5);
        table.addCell(cell);
    }
}