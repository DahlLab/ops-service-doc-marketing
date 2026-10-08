package org.dahllab.opsservicedoc.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RadioCheckField;
import org.dahllab.opsservicedoc.dto.ChecklistDto;
import org.dahllab.opsservicedoc.dto.ChecklistItemDto;
import org.dahllab.opsservicedoc.dto.IpdDocumentDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

// Erzeugt die interne Technikerversion der Checkliste als eigenes PDF.
// Gedacht zum Ausdrucken ODER zum Ausfüllen am Tablet: jede Checkbox
// ist ein echtes PDF-Formularfeld (AcroForm), das sich in einem
// PDF-Reader anklicken lässt, und gleichzeitig mit einem sichtbaren
// Rahmen gezeichnet, damit sie auch auf Papier abhakbar ist.
//
// Dieses Dokument ist bewusst vom Kunden-PDF (IpdPdfGenerator) getrennt:
// der Kunde bekommt die Checkliste nie zu sehen, nur der Techniker.
public class ChecklistPdfGenerator {

    private static final Color NAVY = new Color(10, 25, 49);
    private static final Color CYAN = new Color(0, 188, 212);
    private static final float CHECKBOX_SIZE = 12f;

    private ChecklistPdfGenerator() {
        // Utility-Klasse, keine Instanzen nötig.
    }

    public static byte[] createPdf(IpdDocumentDto ipdDocument, List<ChecklistDto> checklists) {
        Document pdfDocument = new Document(PageSize.A4, 40, 40, 40, 50);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(pdfDocument, output);
            writer.setPageEvent(new FooterEvent());
            pdfDocument.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLD, Color.WHITE);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.WHITE);
            Font checklistsFont = FontFactory.getFont(FontFactory.HELVETICA, 13, Font.BOLD, CYAN);
            Font entryFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK);

            pdfDocument.add(buildHeader(ipdDocument, titleFont, subtitleFont));

            for (ChecklistDto checklist : checklists) {
                Paragraph heading = new Paragraph(checklist.title(), checklistsFont);
                heading.setSpacingBefore(14);
                heading.setSpacingAfter(4);
                pdfDocument.add(heading);
                pdfDocument.add(buildItemTable(writer, checklist, entryFont));
            }

            pdfDocument.close();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Checklisten-PDF konnte nicht erzeugt werden", exception);
        }

        return output.toByteArray();
    }

    private static PdfPTable buildHeader(IpdDocumentDto ipdDocument, Font titleFont, Font subtitleFont) {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(NAVY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(14);

        cell.addElement(new Paragraph("Checkliste – Technikerexemplar", titleFont));
        String reference = ipdDocument.title() != null ? ipdDocument.title() : "IPD-Dokument";
        String customer = ipdDocument.customer() != null && !ipdDocument.customer().isBlank() ? " · Kunde: " + ipdDocument.customer() : "";
        Paragraph subtitle = new Paragraph(reference + customer + " · intern, nicht für den Kunden", subtitleFont);
        subtitle.setSpacingBefore(4);
        cell.addElement(subtitle);

        header.addCell(cell);
        return header;
    }

    // Zweispaltige Tabelle: links die Checkbox, rechts der Text. Die
    // Checkbox-Zelle bekommt ein CellEvent, das an der Zellposition das
    // Formularfeld platziert.
    private static PdfPTable buildItemTable(PdfWriter writer, ChecklistDto checklist, Font entryFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 15f});

        int number = 0;
        for (ChecklistItemDto item : checklist.items()) {
            number++;
            PdfPCell boxCell = new PdfPCell(new Phrase(" "));
            boxCell.setBorder(Rectangle.BOTTOM);
            boxCell.setBorderColor(Color.LIGHT_GRAY);
            boxCell.setFixedHeight(24);
            boxCell.setCellEvent(new CheckboxEvent(writer, "cb_" + checklist.id() + "_" + number, item.done()));

            PdfPCell textCell = new PdfPCell(new Phrase(item.description(), entryFont));
            textCell.setBorder(Rectangle.BOTTOM);
            textCell.setBorderColor(Color.LIGHT_GRAY);
            textCell.setPaddingTop(5);
            textCell.setPaddingBottom(5);

            table.addCell(boxCell);
            table.addCell(textCell);
        }
        return table;
    }

    // Zeichnet den Rahmen (für den Papierausdruck) und legt darüber das
    // anklickbare Formularfeld. Bereits erledigte Punkte sind vorab
    // angehakt.
    private static class CheckboxEvent implements PdfPCellEvent {
        private final PdfWriter writer;
        private final String fieldName;
        private final boolean checked;

        CheckboxEvent(PdfWriter writer, String fieldName, boolean checked) {
            this.writer = writer;
            this.fieldName = fieldName;
            this.checked = checked;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            float links = position.getLeft() + 2;
            float lowerY = position.getBottom() + (position.getHeight() - CHECKBOX_SIZE) / 2;
            Rectangle box = new Rectangle(links, lowerY, links + CHECKBOX_SIZE, lowerY + CHECKBOX_SIZE);

            PdfContentByte lines = canvases[PdfPTable.LINECANVAS];
            lines.saveState();
            lines.setColorStroke(NAVY);
            lines.setLineWidth(0.8f);
            lines.rectangle(box.getLeft(), box.getBottom(), box.getWidth(), box.getHeight());
            lines.stroke();
            lines.restoreState();

            try {
                RadioCheckField field = new RadioCheckField(writer, box, fieldName, "Yes");
                field.setCheckType(RadioCheckField.TYPE_CHECK);
                field.setBorderColor(NAVY);
                field.setChecked(checked);
                writer.addAnnotation(field.getCheckField());
            } catch (IOException | DocumentException exception) {
                throw new IllegalStateException("Checkbox konnte nicht erzeugt werden", exception);
            }
        }
    }

    private static class FooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            Phrase footer = new Phrase("OpsServiceDoc – Checkliste (intern) – Seite " + writer.getPageNumber(), footerFont);
            float centerX = (document.left() + document.right()) / 2;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, footer, centerX,
                    document.bottom() - 20, 0);
        }
    }
}
