package com.businessmanager.service;

import com.businessmanager.entity.Invoice;
import com.businessmanager.entity.LineItem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Génération du PDF d'une facture (OpenPDF). */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.company.name:Business Manager}")
    private String companyName;

    @Value("${app.company.address:}")
    private String companyAddress;

    /** Doit être appelé dans une transaction (collections et relations en chargement différé). */
    public byte[] generate(Invoice invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font muted = FontFactory.getFont(FontFactory.HELVETICA, 9);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph(companyName, bold));
            if (companyAddress != null && !companyAddress.isBlank()) {
                document.add(new Paragraph(companyAddress, muted));
            }

            Paragraph heading = new Paragraph("FACTURE " + invoice.getNumber(), title);
            heading.setSpacingBefore(24f);
            document.add(heading);

            document.add(new Paragraph("Date d'émission : " + date(invoice.getIssueDate()), normal));
            document.add(new Paragraph("Échéance : " + date(invoice.getDueDate()), normal));

            Paragraph billTo = new Paragraph("Facturé à", bold);
            billTo.setSpacingBefore(16f);
            document.add(billTo);
            document.add(new Paragraph(invoice.getCustomer().getName(), normal));
            if (invoice.getCustomer().getCompany() != null) {
                document.add(new Paragraph(invoice.getCustomer().getCompany(), normal));
            }
            if (invoice.getCustomer().getAddress() != null) {
                document.add(new Paragraph(invoice.getCustomer().getAddress(), normal));
            }

            PdfPTable table = new PdfPTable(new float[]{5f, 1.2f, 1.8f, 1.2f, 1.8f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(20f);
            addCell(table, "Description", bold, Element.ALIGN_LEFT);
            addCell(table, "Qté", bold, Element.ALIGN_RIGHT);
            addCell(table, "PU HT", bold, Element.ALIGN_RIGHT);
            addCell(table, "TVA", bold, Element.ALIGN_RIGHT);
            addCell(table, "Total HT", bold, Element.ALIGN_RIGHT);

            for (LineItem line : invoice.getLines()) {
                addCell(table, line.getDescription(), normal, Element.ALIGN_LEFT);
                addCell(table, line.getQuantity().stripTrailingZeros().toPlainString(), normal, Element.ALIGN_RIGHT);
                addCell(table, money(line.getUnitPrice()), normal, Element.ALIGN_RIGHT);
                addCell(table, line.getVatRate().stripTrailingZeros().toPlainString() + " %", normal, Element.ALIGN_RIGHT);
                addCell(table, money(line.getTotalHt()), normal, Element.ALIGN_RIGHT);
            }
            document.add(table);

            addTotal(document, "Total HT : " + money(invoice.getTotalHt()), normal, 16f);
            addTotal(document, "TVA : " + money(invoice.getTotalVat()), normal, 2f);
            addTotal(document, "Total TTC : " + money(invoice.getTotalTtc()), bold, 2f);
            addTotal(document, "Déjà réglé : " + money(invoice.getAmountPaid()), normal, 2f);
            addTotal(document, "Reste à payer : " + money(invoice.getBalance()), bold, 2f);

            if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
                Paragraph notes = new Paragraph(invoice.getNotes(), muted);
                notes.setSpacingBefore(24f);
                document.add(notes);
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF", e);
        }
        return out.toByteArray();
    }

    private static void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private static void addTotal(Document document, String text, Font font, float spacingBefore) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        paragraph.setSpacingBefore(spacingBefore);
        document.add(paragraph);
    }

    private static String date(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMAT);
    }

    static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');   // espace standard (l'espace insécable fine n'existe pas en WinAnsi)
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(value) + " €";
    }
}
