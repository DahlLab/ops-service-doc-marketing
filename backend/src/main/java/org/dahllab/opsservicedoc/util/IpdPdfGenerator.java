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
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.dahllab.opsservicedoc.dto.IpdDocumentDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

// Erzeugt aus einem IpdDocumentDto das fertige PDF, das ich dem Kunden
// zur Freigabe schicke. Ich nutze dafür OpenPDF (Fork von iText 2) -
// die Document/Paragraph/PdfPTable-API reicht völlig aus, um ein
// optisch aufgewertetes Dokument zu bauen (farbiger Kopfbereich,
// Metadaten-Tabelle, Fußzeile mit Seitenzahl), ohne mit
// PDF-Grafikbefehlen auf niedriger Ebene zu arbeiten.
//
// Farbgebung orientiert sich an der DahlLab-Designsprache (Navy +
// dunkles Blau), damit IPD-Generator und Operations Hub optisch
// zusammengehören. Cyan ist im Druck zu grell, daher nutze ich es hier nicht.
//
// WICHTIG: Die interne Checkliste taucht hier bewusst NICHT im Detail
// auf, nur das Ergebnis als ein Satz ("Qualitätssicherung
// durchgeführt: Ja/Nein") - laut echter Praxis bekommt der Kunde die
// Checkliste selbst nie zu sehen, nur ich als Techniker intern.
public class IpdPdfGenerator {

    // Feste Farbpalette, damit ich sie nicht an jeder Stelle neu
    // anlegen muss und überall exakt dieselben Töne verwende.
    private static final Color NAVY = new Color(10, 25, 49);
    // Dunkles Blau für die (unterstrichenen) Überschriften - ruhiger als
    // Cyan und auf Papier gut lesbar.
    private static final Color DARK_BLUE = new Color(23, 48, 92);
    private static final Color PALE_GRAY = new Color(240, 240, 240);

    private IpdPdfGenerator() {
        // Utility-Klasse, keine Instanzen nötig.
    }

    public static byte[] createPdf(IpdDocumentDto ipdDocument) {
        Document pdfDocument = new Document(PageSize.A4, 40, 40, 40, 50);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(pdfDocument, output);
            // Zeichnet auf jeder Seite automatisch die Fußzeile mit
            // Seitenzahl ein, siehe FusszeilenEvent weiter unten.
            writer.setPageEvent(new FooterEvent());
            pdfDocument.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD, Color.WHITE);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.WHITE);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA, 13, Font.BOLD | Font.UNDERLINE, DARK_BLUE);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, NAVY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK);

            pdfDocument.add(buildHeader(ipdDocument, titleFont, subtitleFont));
            pdfDocument.add(newBlankLine());
            pdfDocument.add(buildMetadataTable(ipdDocument, labelFont, valueFont));
            pdfDocument.add(newBlankLine());

            addSection(pdfDocument, "Ausgangslage", ipdDocument.initialSituation(), sectionFont, textFont);
            addSection(pdfDocument, "Anforderungen", ipdDocument.requirements(), sectionFont, textFont);
            addSection(pdfDocument, "Infrastruktur-Übersicht", ipdDocument.infrastructureOverview(), sectionFont, textFont);
            addSection(pdfDocument, "Server und VMs", ipdDocument.serversAndVms(), sectionFont, textFont);
            addSection(pdfDocument, "Netzwerk", ipdDocument.network(), sectionFont, textFont);
            addSection(pdfDocument, "Rollen und Verantwortlichkeiten", ipdDocument.rolesAndResponsibilities(), sectionFont, textFont);
            addSection(pdfDocument, "Backup-Konzept", ipdDocument.backupPlan(), sectionFont, textFont);
            addSection(pdfDocument, "Security-Überlegungen", ipdDocument.securityConsiderations(), sectionFont, textFont);
            addSection(pdfDocument, "Durchgeführte Schritte", ipdDocument.performedSteps(), sectionFont, textFont);
            addSection(pdfDocument, "Entscheidungen", ipdDocument.decisions(), sectionFont, textFont);
            addSection(pdfDocument, "Risiken und Annahmen", ipdDocument.risksAndAssumptions(), sectionFont, textFont);
            addSection(pdfDocument, "Rollback-Plan", ipdDocument.rollbackPlan(), sectionFont, textFont);
            addSection(pdfDocument, "Qualitätssicherung durchgeführt",
                    ipdDocument.qualityAssuranceCompleted() ? "Ja" : "Nein", sectionFont, textFont);

            pdfDocument.close();
        } catch (DocumentException exception) {
            // Laufzeit-Exception statt geprüfter Exception, damit ich
            // sie nicht bis in den Controller durchreichen muss - ein
            // PDF-Erzeugungsfehler ist hier ein echter, unerwarteter
            // Fehlerfall, kein fachlicher 404/400.
            throw new IllegalStateException("PDF konnte nicht erzeugt werden", exception);
        }

        return output.toByteArray();
    }

    // Baut den dunklen Navy-Kopfbereich mit Titel und Untertitel -
    // technisch eine 1x1-Tabelle mit farbigem Zellenhintergrund, weil
    // OpenPDF keinen direkten "farbigen Absatz" kennt, wohl aber
    // farbige Tabellenzellen.
    private static PdfPTable buildHeader(IpdDocumentDto ipdDocument, Font titleFont, Font subtitleFont) {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(NAVY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(16);

        Paragraph title = new Paragraph(ipdDocument.title() != null ? ipdDocument.title() : "IPD-Dokument", titleFont);
        Paragraph subtitle = new Paragraph("IPD-Dokument – OpsServiceDoc", subtitleFont);
        subtitle.setSpacingBefore(4);

        cell.addElement(title);
        cell.addElement(subtitle);
        header.addCell(cell);

        return header;
    }

    // Stellt die wichtigsten Projektinfos (Kunde, Ansprechpartner,
    // Techniker, Szenario, Zeitraum, Status) als zweispaltige Tabelle
    // dar, statt als Fließtext untereinander - wirkt dadurch
    // strukturierter, wie ein echtes Formular-Deckblatt.
    private static PdfPTable buildMetadataTable(IpdDocumentDto ipdDocument, Font labelFont, Font valueFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        // setWidths wirft eine DocumentException, falls die Anzahl der
        // Breitenangaben nicht zur Spaltenzahl passt - hier bewusst
        // 1:2, damit die Werte-Spalte doppelt so breit ist wie die
        // Label-Spalte.
        table.setWidths(new float[]{1f, 2f});

        addMetadataRow(table, "Kunde", ipdDocument.customer(), labelFont, valueFont);
        addMetadataRow(table, "Ansprechpartner", ipdDocument.customerContact(), labelFont, valueFont);
        addMetadataRow(table, "Techniker", ipdDocument.technician(), labelFont, valueFont);
        addMetadataRow(table, "Szenario",
                ipdDocument.scenarioType() != null ? ipdDocument.scenarioType().toString() : null, labelFont, valueFont);
        addMetadataRow(table, "Zeitraum", ipdDocument.period(), labelFont, valueFont);
        addMetadataRow(table, "Status",
                ipdDocument.status() != null ? ipdDocument.status().toString() : null, labelFont, valueFont);

        return table;
    }

    // Fügt eine einzelne Label/Wert-Zeile zur Metadaten-Tabelle hinzu,
    // mit grau hinterlegter Label-Spalte. Leere Werte zeige ich als
    // "-" an, damit keine Zelle einfach leer bleibt.
    private static void addMetadataRow(PdfPTable table, String label, String value,
                                                 Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(PALE_GRAY);
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        labelCell.setPadding(6);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null && !value.isBlank() ? value : "-", valueFont));
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        valueCell.setPadding(6);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // Fügt einen einzelnen Fachabschnitt hinzu: dunkelblaue, unterstrichene
    // Überschrift, dann der eigentliche Text. Leere,
    // noch nicht ausgefüllte Abschnitte lasse ich komplett weg, damit
    // der Kunde kein halbfertiges Dokument mit leeren Überschriften
    // bekommt.
    private static void addSection(Document pdfDocument, String heading, String content,
                                            Font sectionFont, Font textFont) throws DocumentException {
        if (content == null || content.isBlank()) {
            return;
        }

        Paragraph headingParagraph = new Paragraph(heading, sectionFont);
        headingParagraph.setSpacingBefore(10);
        pdfDocument.add(headingParagraph);

        Paragraph textParagraph = new Paragraph(content, textFont);
        textParagraph.setSpacingBefore(4);
        pdfDocument.add(textParagraph);
    }

    private static Paragraph newBlankLine() {
        return new Paragraph(" ");
    }

    // PageEvent, das am Ende jeder Seite eine Fußzeile mit Seitenzahl
    // einzeichnet. OpenPDF ruft onEndPage() automatisch für jede
    // fertiggestellte Seite auf - ich muss mich also um nichts manuell
    // kümmern, sobald der Writer dieses Event kennt (siehe
    // writer.setPageEvent(...) weiter oben in erzeugePdf()).
    private static class FooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            Phrase footer = new Phrase("OpsServiceDoc – Seite " + writer.getPageNumber(), footerFont);

            float centerX = (document.left() + document.right()) / 2;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, footer, centerX,
                    document.bottom() - 20, 0);
        }
    }
}
