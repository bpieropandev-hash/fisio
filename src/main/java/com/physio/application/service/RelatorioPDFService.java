package com.physio.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.physio.domain.model.Atendimento;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private final AtendimentoRepositoryPort repository;
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public byte[] gerarRelatorioFinanceiro(int mes, int ano) {
        LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0);
        LocalDateTime fim = inicio.plusMonths(1).minusSeconds(1);
        
        List<Atendimento> atendimentos = repository.listarPorPeriodo(inicio, fim);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Relatório Financeiro - " + mes + "/" + ano, fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // Tabela
            PdfPTable table = new PdfPTable(4); // Data, Paciente, Valor, Profissional
            table.setWidthPercentage(100);
            
            // Cabeçalho
            addCell(table, "Data", true);
            addCell(table, "Paciente", true);
            addCell(table, "Valor Total", true);
            addCell(table, "Valor Prof.", true);

            BigDecimal totalGeral = BigDecimal.ZERO;
            BigDecimal totalProf = BigDecimal.ZERO;

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");

            for (Atendimento a : atendimentos) {
                addCell(table, a.getDataHoraInicio().format(fmt), false);
                addCell(table, a.getPaciente().getNome(), false);
                
                // Cálculos
                BigDecimal valor = a.getValorCobrado();
                BigDecimal parteProf = valor.multiply(a.getPctProfissionalSnapshot()).divide(new BigDecimal(100));

                addCell(table, nf.format(valor), false);
                addCell(table, nf.format(parteProf), false);
                
                totalGeral = totalGeral.add(valor);
                totalProf = totalProf.add(parteProf);
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Totais
            document.add(new Paragraph("Faturamento Total: R$ " + nf.format(totalGeral)));
            document.add(new Paragraph("Repasse Profissional: R$ " + nf.format(totalProf)));
            document.add(new Paragraph("Lucro Clínica: R$ " + nf.format(totalGeral.subtract(totalProf))));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        if (header) {
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        }
        table.addCell(cell);
    }
}