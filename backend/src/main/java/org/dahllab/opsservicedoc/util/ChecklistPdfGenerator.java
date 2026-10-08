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
    private static final float CHECKBOX_GROESSE = 12f;

    private ChecklistPdfGenerator() {
        // Utility-Klasse, keine Instanzen nötig.
    }

    public static byte[] erzeugePdf(IpdDocumentDto dokument, List<ChecklistDto> checklisten) {
        Document pdfDokument = new Document(PageSize.A4, 40, 40, 40, 50);
        ByteArrayOutputStream ausgabe = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(pdfDokument, ausgabe);
            writer.setPageEvent(new FusszeilenEvent());
            pdfDokument.open();

            Font titelFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLD, Color.WHITE);
            Font untertitelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.WHITE);
            Font checklistenFont = FontFactory.getFont(FontFactory.HELVETICA, 13, Font.BOLD, CYAN);
            Font punktFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK);

            pdfDokument.add(baueKopfbereich(dokument, titelFont, untertitelFont));

            for (ChecklistDto checkliste : checklisten) {
                Paragraph ueberschrift = new Paragraph(checkliste.titel(), checklistenFont);
                ueberschrift.setSpacingBefore(14);
                ueberschrift.setSpacingAfter(4);
                pdfDokument.add(ueberschrift);
                pdfDokument.add(baueItemTabelle(writer, checkliste, punktFont));
            }

            pdfDokument.close();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Checklisten-PDF konnte nicht erzeugt werden", exception);
        }

        return ausgabe.toByteArray();
    }

    private static PdfPTable baueKopfbereich(IpdDocumentDto dokument, Font titelFont, Font untertitelFont) {
        PdfPTable kopfbereich = new PdfPTable(1);
        kopfbereich.setWidthPercentage(100);

        PdfPCell zelle = new PdfPCell();
        zelle.setBackgroundColor(NAVY);
        zelle.setBorder(Rectangle.NO_BORDER);
        zelle.setPadding(14);

        zelle.addElement(new Paragraph("Checkliste – Technikerexemplar", titelFont));
        String bezug = dokument.titel() != null ? dokument.titel() : "IPD-Dokument";
        String kunde = dokument.kunde() != null && !dokument.kunde().isBlank() ? " · Kunde: " + dokument.kunde() : "";
        Paragraph untertitel = new Paragraph(bezug + kunde + " · intern, nicht für den Kunden", untertitelFont);
        untertitel.setSpacingBefore(4);
        zelle.addElement(untertitel);

        kopfbereich.addCell(zelle);
        return kopfbereich;
    }

    // Zweispaltige Tabelle: links die Checkbox, rechts der Text. Die
    // Checkbox-Zelle bekommt ein CellEvent, das an der Zellposition das
    // Formularfeld platziert.
    private static PdfPTable baueItemTabelle(PdfWriter writer, ChecklistDto checkliste, Font punktFont)
            throws DocumentException {
        PdfPTable tabelle = new PdfPTable(2);
        tabelle.setWidthPercentage(100);
        tabelle.setWidths(new float[]{1f, 15f});

        int nummer = 0;
        for (ChecklistItemDto item : checkliste.items()) {
            nummer++;
            PdfPCell boxZelle = new PdfPCell(new Phrase(" "));
            boxZelle.setBorder(Rectangle.BOTTOM);
            boxZelle.setBorderColor(Color.LIGHT_GRAY);
            boxZelle.setFixedHeight(24);
            boxZelle.setCellEvent(new CheckboxEvent(writer, "cb_" + checkliste.id() + "_" + nummer, item.erledigt()));

            PdfPCell textZelle = new PdfPCell(new Phrase(item.beschreibung(), punktFont));
            textZelle.setBorder(Rectangle.BOTTOM);
            textZelle.setBorderColor(Color.LIGHT_GRAY);
            textZelle.setPaddingTop(5);
            textZelle.setPaddingBottom(5);

            tabelle.addCell(boxZelle);
            tabelle.addCell(textZelle);
        }
        return tabelle;
    }

    // Zeichnet den Rahmen (für den Papierausdruck) und legt darüber das
    // anklickbare Formularfeld. Bereits erledigte Punkte sind vorab
    // angehakt.
    private static class CheckboxEvent implements PdfPCellEvent {
        private final PdfWriter writer;
        private final String feldName;
        private final boolean angehakt;

        CheckboxEvent(PdfWriter writer, String feldName, boolean angehakt) {
            this.writer = writer;
            this.feldName = feldName;
            this.angehakt = angehakt;
        }

        @Override
        public void cellLayout(PdfPCell zelle, Rectangle position, PdfContentByte[] canvases) {
            float links = position.getLeft() + 2;
            float unten = position.getBottom() + (position.getHeight() - CHECKBOX_GROESSE) / 2;
            Rectangle box = new Rectangle(links, unten, links + CHECKBOX_GROESSE, unten + CHECKBOX_GROESSE);

            PdfContentByte linien = canvases[PdfPTable.LINECANVAS];
            linien.saveState();
            linien.setColorStroke(NAVY);
            linien.setLineWidth(0.8f);
            linien.rectangle(box.getLeft(), box.getBottom(), box.getWidth(), box.getHeight());
            linien.stroke();
            linien.restoreState();

            try {
                RadioCheckField feld = new RadioCheckField(writer, box, feldName, "Yes");
                feld.setCheckType(RadioCheckField.TYPE_CHECK);
                feld.setBorderColor(NAVY);
                feld.setChecked(angehakt);
                writer.addAnnotation(feld.getCheckField());
            } catch (IOException | DocumentException exception) {
                throw new IllegalStateException("Checkbox konnte nicht erzeugt werden", exception);
            }
        }
    }

    private static class FusszeilenEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font fussFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            Phrase fusszeile = new Phrase("OpsServiceDoc – Checkliste (intern) – Seite " + writer.getPageNumber(), fussFont);
            float mitteX = (document.left() + document.right()) / 2;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, fusszeile, mitteX,
                    document.bottom() - 20, 0);
        }
    }
}
