package com.physio.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.physio.domain.model.CobrancaMensal;
import com.physio.domain.model.Recebedor;
import com.physio.domain.ports.out.CobrancaMensalRepositoryPort;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.entity.PacienteEntity;
import com.physio.infrastructure.out.persistence.repository.AtendimentoJpaRepository;
import com.physio.infrastructure.out.persistence.repository.PacienteJpaRepository;
import lombok.RequiredArgsConstructor;
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

    private final AtendimentoJpaRepository repository;
    private final CobrancaMensalRepositoryPort cobrancaMensalRepositoryPort;
    private final PacienteJpaRepository pacienteRepository;

    public byte[] gerarRelatorioPersonalizado(LocalDateTime inicio, LocalDateTime fim, List<Integer> servicoIds) {
        // 1. Busca atendimentos com valor > 0 (fisioterapia avulsa)
        List<AtendimentoEntity> atendimentos = repository.findParaRelatorioFinanceiro(inicio, fim, servicoIds)
                .stream()
                .filter(a -> a.getValorCobrado().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        // 2. Busca cobranças mensais pagas no período
        // Converter LocalDateTime para mês/ano
        int mesInicio = inicio.getMonthValue();
        int anoInicio = inicio.getYear();
        int mesFim = fim.getMonthValue();
        int anoFim = fim.getYear();
        
        List<CobrancaMensal> cobrancasMensais = cobrancaMensalRepositoryPort.buscarPagasPorPeriodo(
                anoInicio, mesInicio, anoFim, mesFim
        );
        
        // Filtrar cobranças por serviços se necessário
        if (servicoIds != null && !servicoIds.isEmpty()) {
            cobrancasMensais = cobrancasMensais.stream()
                    .filter(c -> servicoIds.contains(c.getAssinatura().getServico().getId()))
                    .toList();
        }

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
            // Colunas: Data | Paciente | Serviço | % Clínica | % Profissional | Pagamento (Info) | Acerto (Valor)
            PdfPTable table = new PdfPTable(new float[]{ 2f, 4f, 3f, 2f, 2f, 4f, 3f });
            table.setWidthPercentage(100);

            // Cabeçalho estilizado
            addHeaderCell(table, "Data");
            addHeaderCell(table, "Paciente");
            addHeaderCell(table, "Serviço");
            addHeaderCell(table, "% Clínica");
            addHeaderCell(table, "% Profissional");
            addHeaderCell(table, "Forma Pagto.");
            addHeaderCell(table, "Crédito/Débito"); // O que entra/sai para a profissional

            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("dd/MM HH:mm");

            BigDecimal saldoFinalProfissional = BigDecimal.ZERO;

            // 3. Criar lista consolidada de itens financeiros (DTO interno)
            List<ItemFinanceiro> itensFinanceiros = new ArrayList<>();
            
            // Adicionar atendimentos
            for (AtendimentoEntity a : atendimentos) {
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
                        true // é atendimento
                ));
            }
            
            // Adicionar cobranças mensais
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
                        null, // não tem hora
                        false // é cobrança mensal
                ));
            }
            
            // Ordenar por data
            itensFinanceiros.sort(Comparator.comparing(ItemFinanceiro::getData));

            // 4. Processar itens consolidados
            for (ItemFinanceiro item : itensFinanceiros) {
                // Preencher Colunas Básicas
                if (item.isAtendimento() && item.getDataHora() != null) {
                    addCell(table, item.getDataHora().format(fmtHora));
                } else {
                    addCell(table, item.getData().format(fmtData));
                }
                addCell(table, item.getPaciente());
                addCell(table, item.getServico());

                // Porcentagens
                addCell(table, formatPercent(item.getPctClinica()));
                addCell(table, formatPercent(item.getPctProfissional()));

                // Lógica de Exibição do Pagamento
                String infoPagto = nf.format(item.getValor()) + "\n" +
                        item.getTipoPagamento() + " " +
                        (item.getRecebedor() != null ? item.getRecebedor().toString() : "");
                addCell(table, infoPagto);

                // --- A LÓGICA DO ACERTO FINANCEIRO ---
                BigDecimal valorTotal = item.getValor();
                BigDecimal parteProf = valorTotal.multiply(item.getPctProfissional()).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal parteClinica = valorTotal.multiply(item.getPctClinica()).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);

                if (item.getRecebedor() == Recebedor.CLINICA) {
                    // Clínica recebeu tudo. Clínica deve pagar a parte da profissional.
                    // EFEITO: + Crédito para a profissional
                    BigDecimal valorAcerto = parteProf;
                    saldoFinalProfissional = saldoFinalProfissional.add(valorAcerto);

                    // Visual na tabela (Positivo em azul ou preto)
                    addCell(table, "+ " + nf.format(valorAcerto));

                } else if (item.getRecebedor() == Recebedor.PROFISSIONAL) {
                    // Profissional recebeu tudo (100%).
                    // Mas ela só é dona da 'parteProf'. Ela está segurando a 'parteClinica'.
                    // No acerto, devemos descontar a parte da clínica do que ela tem a receber de outros atendimentos.
                    // EFEITO: - Débito para a profissional
                    BigDecimal valorAcerto = parteClinica.negate(); // Negativo
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

    public byte[] gerarProntuario(Long pacienteId) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Busca paciente explicitamente
            PacienteEntity paciente = pacienteRepository.findById(Math.toIntExact(pacienteId))
                    .orElse(null);

            // Buscar atendimentos do paciente
            List<AtendimentoEntity> atendimentos = repository.findByPaciente_Id(Math.toIntExact(pacienteId)).stream()
                    .filter(a -> "CONCLUIDO".equalsIgnoreCase(a.getStatus()))
                    .sorted(Comparator.comparing(AtendimentoEntity::getDataHoraInicio))
                    .collect(Collectors.toList());

            // Cabeçalho: nome e data nascimento (mesmo que não haja atendimentos)
            String nome = paciente != null ? paciente.getNome() : "-";
            String dataNasc = paciente != null && paciente.getDataNascimento() != null ?
                    paciente.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";

            Paragraph cabecalho = new Paragraph(nome + " - Nasc.: " + dataNasc, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            cabecalho.setAlignment(Element.ALIGN_LEFT);
            document.add(cabecalho);
            document.add(Chunk.NEWLINE);

            if (atendimentos.isEmpty()) {
                Paragraph vazio = new Paragraph("Prontuário - Nenhum atendimento concluído encontrado.", FontFactory.getFont(FontFactory.HELVETICA, 12));
                vazio.setAlignment(Element.ALIGN_CENTER);
                document.add(vazio);
                document.close();
                return out.toByteArray();
            }

            // Corpo: lista cronológica
            DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (AtendimentoEntity a : atendimentos) {
                Paragraph p = new Paragraph();
                p.add(new Paragraph(a.getDataHoraInicio().format(fmtHora) + " - " + (a.getServicoBase() != null ? a.getServicoBase().getNome() : "-"), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                p.add(new Paragraph("Evolução:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                p.add(new Paragraph(a.getEvolucao() != null ? a.getEvolucao() : "--", FontFactory.getFont(FontFactory.HELVETICA, 11)));
                p.add(Chunk.NEWLINE);
                document.add(p);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar prontuário", e);
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

    // Classe auxiliar interna para consolidar atendimentos e cobranças mensais
    private static class ItemFinanceiro {
        private LocalDate data;
        private String paciente;
        private String servico;
        private BigDecimal valor;
        private String tipoPagamento;
        private Recebedor recebedor;
        private BigDecimal pctClinica;
        private BigDecimal pctProfissional;
        private LocalDateTime dataHora;
        private boolean isAtendimento;

        public ItemFinanceiro(LocalDate data, String paciente, String servico, BigDecimal valor,
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

        public LocalDate getData() { return data; }
        public String getPaciente() { return paciente; }
        public String getServico() { return servico; }
        public BigDecimal getValor() { return valor; }
        public String getTipoPagamento() { return tipoPagamento; }
        public Recebedor getRecebedor() { return recebedor; }
        public BigDecimal getPctClinica() { return pctClinica; }
        public BigDecimal getPctProfissional() { return pctProfissional; }
        public LocalDateTime getDataHora() { return dataHora; }
        public boolean isAtendimento() { return isAtendimento; }
    }
}
