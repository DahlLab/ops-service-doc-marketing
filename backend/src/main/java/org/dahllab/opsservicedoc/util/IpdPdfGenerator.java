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
    private static final Color DUNKELBLAU = new Color(23, 48, 92);
    private static final Color HELLGRAU = new Color(240, 240, 240);

    private IpdPdfGenerator() {
        // Utility-Klasse, keine Instanzen nötig.
    }

    public static byte[] erzeugePdf(IpdDocumentDto dokument) {
        Document pdfDokument = new Document(PageSize.A4, 40, 40, 40, 50);
        ByteArrayOutputStream ausgabe = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(pdfDokument, ausgabe);
            // Zeichnet auf jeder Seite automatisch die Fußzeile mit
            // Seitenzahl ein, siehe FusszeilenEvent weiter unten.
            writer.setPageEvent(new FusszeilenEvent());
            pdfDokument.open();

            Font titelFont = FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD, Color.WHITE);
            Font untertitelFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.WHITE);
            Font abschnittFont = FontFactory.getFont(FontFactory.HELVETICA, 13, Font.BOLD | Font.UNDERLINE, DUNKELBLAU);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, NAVY);
            Font wertFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK);

            pdfDokument.add(baueKopfbereich(dokument, titelFont, untertitelFont));
            pdfDokument.add(neueLeerzeile());
            pdfDokument.add(baueMetadatenTabelle(dokument, labelFont, wertFont));
            pdfDokument.add(neueLeerzeile());

            fuegeAbschnittHinzu(pdfDokument, "Ausgangslage", dokument.ausgangslage(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Anforderungen", dokument.anforderungen(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Infrastruktur-Übersicht", dokument.infrastrukturUebersicht(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Server und VMs", dokument.serverUndVms(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Netzwerk", dokument.netzwerk(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Rollen und Verantwortlichkeiten", dokument.rollenUndVerantwortlichkeiten(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Backup-Konzept", dokument.backupKonzept(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Security-Überlegungen", dokument.securityUeberlegungen(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Durchgeführte Schritte", dokument.durchgefuehrteSchritte(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Entscheidungen", dokument.entscheidungen(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Risiken und Annahmen", dokument.risikenUndAnnahmen(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Rollback-Plan", dokument.rollbackPlan(), abschnittFont, textFont);
            fuegeAbschnittHinzu(pdfDokument, "Qualitätssicherung durchgeführt",
                    dokument.qualitaetssicherungAbgeschlossen() ? "Ja" : "Nein", abschnittFont, textFont);

            pdfDokument.close();
        } catch (DocumentException exception) {
            // Laufzeit-Exception statt geprüfter Exception, damit ich
            // sie nicht bis in den Controller durchreichen muss - ein
            // PDF-Erzeugungsfehler ist hier ein echter, unerwarteter
            // Fehlerfall, kein fachlicher 404/400.
            throw new IllegalStateException("PDF konnte nicht erzeugt werden", exception);
        }

        return ausgabe.toByteArray();
    }

    // Baut den dunklen Navy-Kopfbereich mit Titel und Untertitel -
    // technisch eine 1x1-Tabelle mit farbigem Zellenhintergrund, weil
    // OpenPDF keinen direkten "farbigen Absatz" kennt, wohl aber
    // farbige Tabellenzellen.
    private static PdfPTable baueKopfbereich(IpdDocumentDto dokument, Font titelFont, Font untertitelFont) {
        PdfPTable kopfbereich = new PdfPTable(1);
        kopfbereich.setWidthPercentage(100);

        PdfPCell zelle = new PdfPCell();
        zelle.setBackgroundColor(NAVY);
        zelle.setBorder(Rectangle.NO_BORDER);
        zelle.setPadding(16);

        Paragraph titel = new Paragraph(dokument.titel() != null ? dokument.titel() : "IPD-Dokument", titelFont);
        Paragraph untertitel = new Paragraph("IPD-Dokument – OpsServiceDoc", untertitelFont);
        untertitel.setSpacingBefore(4);

        zelle.addElement(titel);
        zelle.addElement(untertitel);
        kopfbereich.addCell(zelle);

        return kopfbereich;
    }

    // Stellt die wichtigsten Projektinfos (Kunde, Ansprechpartner,
    // Techniker, Szenario, Zeitraum, Status) als zweispaltige Tabelle
    // dar, statt als Fließtext untereinander - wirkt dadurch
    // strukturierter, wie ein echtes Formular-Deckblatt.
    private static PdfPTable baueMetadatenTabelle(IpdDocumentDto dokument, Font labelFont, Font wertFont)
            throws DocumentException {
        PdfPTable tabelle = new PdfPTable(2);
        tabelle.setWidthPercentage(100);
        // setWidths wirft eine DocumentException, falls die Anzahl der
        // Breitenangaben nicht zur Spaltenzahl passt - hier bewusst
        // 1:2, damit die Werte-Spalte doppelt so breit ist wie die
        // Label-Spalte.
        tabelle.setWidths(new float[]{1f, 2f});

        fuegeMetadatenZeileHinzu(tabelle, "Kunde", dokument.kunde(), labelFont, wertFont);
        fuegeMetadatenZeileHinzu(tabelle, "Ansprechpartner", dokument.ansprechpartnerKunde(), labelFont, wertFont);
        fuegeMetadatenZeileHinzu(tabelle, "Techniker", dokument.techniker(), labelFont, wertFont);
        fuegeMetadatenZeileHinzu(tabelle, "Szenario",
                dokument.szenarioTyp() != null ? dokument.szenarioTyp().toString() : null, labelFont, wertFont);
        fuegeMetadatenZeileHinzu(tabelle, "Zeitraum", dokument.zeitraum(), labelFont, wertFont);
        fuegeMetadatenZeileHinzu(tabelle, "Status",
                dokument.status() != null ? dokument.status().toString() : null, labelFont, wertFont);

        return tabelle;
    }

    // Fügt eine einzelne Label/Wert-Zeile zur Metadaten-Tabelle hinzu,
    // mit grau hinterlegter Label-Spalte. Leere Werte zeige ich als
    // "-" an, damit keine Zelle einfach leer bleibt.
    private static void fuegeMetadatenZeileHinzu(PdfPTable tabelle, String label, String wert,
                                                 Font labelFont, Font wertFont) {
        PdfPCell labelZelle = new PdfPCell(new Phrase(label, labelFont));
        labelZelle.setBackgroundColor(HELLGRAU);
        labelZelle.setBorderColor(Color.LIGHT_GRAY);
        labelZelle.setPadding(6);

        PdfPCell wertZelle = new PdfPCell(new Phrase(wert != null && !wert.isBlank() ? wert : "-", wertFont));
        wertZelle.setBorderColor(Color.LIGHT_GRAY);
        wertZelle.setPadding(6);

        tabelle.addCell(labelZelle);
        tabelle.addCell(wertZelle);
    }

    // Fügt einen einzelnen Fachabschnitt hinzu: dunkelblaue, unterstrichene
    // Überschrift, dann der eigentliche Text. Leere,
    // noch nicht ausgefüllte Abschnitte lasse ich komplett weg, damit
    // der Kunde kein halbfertiges Dokument mit leeren Überschriften
    // bekommt.
    private static void fuegeAbschnittHinzu(Document pdfDokument, String ueberschrift, String inhalt,
                                            Font abschnittFont, Font textFont) throws DocumentException {
        if (inhalt == null || inhalt.isBlank()) {
            return;
        }

        Paragraph ueberschriftAbsatz = new Paragraph(ueberschrift, abschnittFont);
        ueberschriftAbsatz.setSpacingBefore(10);
        pdfDokument.add(ueberschriftAbsatz);

        Paragraph textAbsatz = new Paragraph(inhalt, textFont);
        textAbsatz.setSpacingBefore(4);
        pdfDokument.add(textAbsatz);
    }

    private static Paragraph neueLeerzeile() {
        return new Paragraph(" ");
    }

    // PageEvent, das am Ende jeder Seite eine Fußzeile mit Seitenzahl
    // einzeichnet. OpenPDF ruft onEndPage() automatisch für jede
    // fertiggestellte Seite auf - ich muss mich also um nichts manuell
    // kümmern, sobald der Writer dieses Event kennt (siehe
    // writer.setPageEvent(...) weiter oben in erzeugePdf()).
    private static class FusszeilenEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font fussFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            Phrase fusszeile = new Phrase("OpsServiceDoc – Seite " + writer.getPageNumber(), fussFont);

            float mitteX = (document.left() + document.right()) / 2;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, fusszeile, mitteX,
                    document.bottom() - 20, 0);
        }
    }
}
