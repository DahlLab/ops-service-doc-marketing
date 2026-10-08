package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.model.SzenarioTyp;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

// Wandelt die rohen GLPI-API-Antworten (generische Map<String, Object>,
// wie sie von GlpiClient.getAllGlpiTickets() zurückkommen) in mein
// eigenes Ticket-Model um. Hält diese Umwandlungs-Logik zentral an
// EINER Stelle, statt sie im Service oder Controller zu wiederholen (DRY).
public class GlpiTicketMapper {

    // GLPI liefert das Erstellungsdatum in diesem Format zurück
    // (z.b. "2026-09-21 14:30:00"), eigenes Formatter-Objekt, damit
    // ich es nicht bei jedem Aufruf neu erzeugen muss.
    private static final DateTimeFormatter GLPI_DATE_FORMAT=
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // GLPI liefert im Standard-Ticket-Objekt keinen lesbaren Techniker-Namen,
    // nur eine User-ID (users_id_recipient bzw. eine separate Zuweisungs-
    // Tabelle). Eine echte Namensaufloesung wuerde einen zusaetzlichen
    // API-Aufruf pro Ticket bedeuten, das hebe ich fuer einen spaeteren
    // Ausbauschritt auf (YAGNI: erst bauen, wenn der MVP es wirklich braucht).
    // Bis dahin steht hier eine feste Konstante statt einer Methode.
    private static final String TECHNIKER_NICHT_ZUGEWIESEN = "Nicht zugewiesen";

    // Privater Konstruktor: reine Utility-Klasse, nur statische Methoden,
    // soll nicht instanziiert werden.
    private GlpiTicketMapper() {
    }

    // Wandelt ein einzelnes rohes GLPI-Ticket in mein Ticket-Model um.
    public static Ticket toTicket(Map<String, Object> glpiTicket) {
        return new Ticket(
                null,     // MongoDB generiert die ID selbst, dieses Ticket ist neu für MEINE Datenbank
                extractGlpiId(glpiTicket),
                extractTitel(glpiTicket),
                extractBeschreibung(glpiTicket),
                extractStatus(glpiTicket),
                TECHNIKER_NICHT_ZUGEWIESEN,
                // SzenarioTyp ist ein Feld, das GLPI selbst nicht kennt, es gehört
                // zu meiner eigenen IPD-Fachlogik. Laut aktuellem Projekt-Scope
                // gibt es bisher nur EIN Szenario (SERVER_WARTUNG), deshalb hier
                // fest gesetzt statt aus GLPI-Daten abgeleitet (YAGNI: keine
                // Szenario-Erkennungslogik bauen, bevor es mehrere Szenarien gibt).
                SzenarioTyp.SERVER_WARTUNG,
                extractErstelltAm(glpiTicket)
        );
    }

    // GLPI liefert die Ticket-ID als Zahl (Integer/Long), ich speichere sie
    // aber als String in glpiTicketId (konsistent mit meiner eigenen
    // MongoDB-Id, die ebenfalls ein String ist).
    private static String extractGlpiId(Map<String, Object> glpiTicket) {
        Object id = glpiTicket.get("id");
        return id != null ? id.toString() : null;
    }

    private static String extractTitel(Map<String, Object> glpiTicket) {
        Object name = glpiTicket.get("name");
        return name != null ? name.toString() : "";
    }

    private static String extractBeschreibung(Map<String, Object> glpiTicket) {
        Object content = glpiTicket.get("content");
        return content != null ? content.toString() : "";
    }

    // Wandelt den numerischen GLPI-Status (1-6) in mein eigenes
    // TicketStatus-Enum um. Die Zuordnung entspricht den offiziellen
    // GLPI-ITILObject-Statuskonstanten:
    // 1=Neu, 2,3=In Bearbeitung, 4=Ausstehend, 5=Gelöst, 6=Geschlossen
    private static TicketStatus extractStatus(Map<String, Object> glpiTicket) {
        Object statusValue = glpiTicket.get("status");
        if (statusValue == null) {
            return TicketStatus.NEU;
        }

        int status = ((Number) statusValue).intValue();

        return switch (status) {
            case 1 -> TicketStatus.NEU;
            case 2, 3 -> TicketStatus.IN_BEARBEITUNG;
            case 4 -> TicketStatus.AUSSTEHEND;
            case 5 -> TicketStatus.GELOEST;
            case 6 -> TicketStatus.GESCHLOSSEN;
            // Unbekannter/neuer GLPI-Status: sicherer Standardwert statt
            // eine Exception zu werfen, damit ein unerwarteter Status-Code
            // nicht den kompletten Ticket-Import zum Absturz bringt.
            default -> TicketStatus.NEU;
        };
    }

    // Wandelt das GLPI-Datumsformat in ein LocalDataTime um.
    // Fällt bei fehlendem oder unlesbarem Datum auf "jetzt" zurück,
    // statt den ganzen Import wegen eines einzelnen Tickets abzubrechen.
    private static LocalDateTime extractErstelltAm(Map<String, Object> glpiTicket) {
        Object date = glpiTicket.get("date");
        if (date == null) {
            return LocalDateTime.now(ZoneId.systemDefault());
        }

        try {
            return LocalDateTime.parse(date.toString(), GLPI_DATE_FORMAT);
        } catch (Exception _) {
            // Unbenannte Variable "_": die Ausnahme selbst brauche ich nicht, ich falle nur auf "jetzt" zurueck.
            return LocalDateTime.now(ZoneId.systemDefault());
        }

    }

}
