package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// @Document: MongoDB-Dokument in der Collection "ipd_documents".
//
// Ein IpdDocument ist die eigentliche Dokumentation, die ich am Ende
// eines Wartungseinsatzes an den Kunden schicke (IPD = angelehnt an
// Microsofts "Infrastructure Planning and Design"-Guide-Reihe). Ich
// bilde die rund 20 fachlichen Abschnitte als einzelne Felder ab,
// statt sie z.B. in einer Map oder einem großen Freitextfeld zu
// bündeln - so bleibt jeder Abschnitt einzeln editierbar und im
// PDF-Export einzeln ansteuerbar.
//
// Ein Teil der Felder befülle ich automatisch beim Erzeugen aus dem
// zugehörigen Ticket bzw. dessen Tasks/Checklisten (titel, techniker,
// szenarioTyp, durchgefuehrteSchritte, qualitaetssicherungAbgeschlossen),
// den Rest trage ich anschließend manuell per PUT ein, bevor das
// Dokument als PDF an den Kunden geht.
//
// WICHTIG: Die internen Checklisten-Inhalte selbst tauchen hier
// bewusst NICHT auf, nur das Endergebnis als einzelner Boolean - laut
// echter Praxis bekommt der Kunde die Checkliste nie zu sehen, nur
// ich als Techniker intern.
@Document(collection = "ipd_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpdDocument {

    @Id
    private String id;

    // Referenz auf die MongoDB-ID des zugehörigen Tickets, gleiches
    // Muster wie bei Task.ticketId und Checklist.ticketId.
    private String ticketId;

    private IpdDocumentStatus status;

    // Automatisch aus dem Ticket übernommene Felder, nicht vom
    // Cleint überschreibbar (siehe IpdDocumentService)
    private String titel;
    private String techniker;
    private SzenarioTyp szenarioTyp;

    // Vom Techniker manuell einzutragende Projektinfos
    private String kunde;
    private String ansprechpartnerKunde;
    private String zeitraum;

    // Die eigentliche IPD-Fachabschnitte, manuell von mir
    // gepflegt
    private String ausgangslage;
    private String anforderungen;
    private String infrastrukturUebersicht;
    private String serverUndVms;
    private String netzwerk;
    private String rollenUndVerantwortlichkeiten;
    private String backupKonzept;
    private String securityUeberlegungen;

    // Automatisch aus den erledigten Tasks des Tickets
    // zusammengesetzter Text (siehe
    // IpdDocumentService.baueDurchgefuehrteSchritteText).
    private String durchgefuehrteSchritte;

    private String entscheidungen;
    private String risikenUndAnnahmen;
    private String rollbackPlan;

    // Automatisch ermittelt: true, sobald zu diesem Ticket mindestens
    // eome Checkliste existiert UND alle zugehörigen Checklisten
    // abgeschlossen sind (siehe
    // IpdDocumentService.ermitteltQualitaetssicherungAbgeschlossen).
    private boolean qualitaetssicherungAbgeschlossen;

    private LocalDateTime erstelltAm;
    private LocalDateTime aktualisiertAm;

}
