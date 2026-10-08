package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.model.SzenarioTyp;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Kein Spring-Kontext nötig, da GlpiTicketMapper eine reine, statische
// Utility-Klasse ist, ich teste hier nur die Umwandlungslogik selbst,
// ganz ohne Datenbank oder HTTP (KISS: minimaler, schneller Test).
class GlpiTicketMapperTest {


    @Test
    @DisplayName("GIVEN ein vollständiges GLPI-Ticket WHEN toTicket aufgerufen wird THEN werden alle Felder korrekt übernommen")
    void toTicket_uebernimmtAlleFelder_wennVollstaendigesGlpiTicketsVorliegt() {

        // GIVEN: Ich baue mit eine Map, die genauso aussieht wie ein einzelnes
        // Ticket-Objekt, das die GLPI-API tatsächlich zurückliefern würde.
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1001");
        glpiTicket.put("name", "Server Enterprise-01 Wartung");
        glpiTicket.put("content", "Geplantes Patching ausserhalb der Betriebszeiten.");
        glpiTicket.put("status", 2); // entspricht IN_BEARBEITUNG laut Mapper
        glpiTicket.put("date", "2026-09-22 09:15:00");

        // WHEN: Ich rufe die zu testende Methode auf.
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        // THEN: Ich prüfe, dass jedes Feld korrekt aus der GLPI-Map übernommen
        // bzw. richtig umgewandelt wurde.
        assertNull(result.getId());   // MongoDB-ID ist noch nicht vergeben
        assertEquals("1001", result.getGlpiTicketId());
        assertEquals("Server Enterprise-01 Wartung", result.getTitel());
        assertEquals("Geplantes Patching ausserhalb der Betriebszeiten.", result.getBeschreibung());
        assertEquals(TicketStatus.IN_BEARBEITUNG, result.getStatus());
        assertEquals(SzenarioTyp.SERVER_WARTUNG, result.getSzenarioTyp());
        assertEquals(LocalDateTime.of(2026, 9, 22, 9,15, 0), result.getErstelltAm());

    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket mit unbekanntem Status WHEN toTicket aufgerufen wird THEN wird NEU als Fallback verwendet")
    void toTicket_verwendetNeuAlsFallback_wennStatusUnbekanntIst() {

        // GIVEN: Ich simuliere einen Status-Code, den mein Mapper nicht kennt
        // (z.b. weil GLPI in einer neueren Version einen zusätzlichen
        // Status eingeführt hat).
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1002");
        glpiTicket.put("name", "Unbekanntes Ticket");
        glpiTicket.put("status", 99);

        // WHEN:
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        // THEN: Ich erwarte den sicheren Standardwert NEU statt einer Exception.
        assertEquals(TicketStatus.NEU, result.getStatus());

    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket ohne Status-Feld WHEN toTicket aufgerufen wird THEN wird NEU als Fallback verwendet")
    void toTicket_verwendetNeuAlsFallback_wennStatusFeldFehlt() {

        // GIVEN: Ich lasse das status-Feld komplett weg, um zu prüfen, dass mein
        // Mapper auch bei fehlenden (nicht nur unbekannten) Werten nicht abstürzt.
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1003");
        glpiTicket.put("name", "Ticket ohne Status");

        // WHEN:
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        // THEN:
        assertEquals(TicketStatus.NEU, result.getStatus());
    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket mit unlesbarem Datum WHEN toTicket aufgerufen wird THEN wird das aktuelle Datum als Fallback verwendet")
    void toTicket_verwendetAktuellesDatumAlsFallback_wennDatumUnlesbareIst() {

        // GIVEN: Ich simuliere ein kaputtes/unerwartetes Datumsformat.
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1004");
        glpiTicket.put("name", "Ticket mit kaputtem Datum");
        glpiTicket.put("date", "kein-gueltiges-datum");

        LocalDateTime vorDemAufruf = LocalDateTime.now();

        // WHEN:
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        LocalDateTime nachDemAufruf = LocalDateTime.now();

        // THEN: Ich kann das exakte "jetzt" nicht vorhersagen, prüfe aber,
        // dass der Fallback-Zeitpunkt zwischen meinen beiden Messungen liegt,
        // das bestätigt, dass wirklich LocalDateTime.now() verwendet wurde.
        assertTrue(!result.getErstelltAm().isBefore(vorDemAufruf)
                && !result.getErstelltAm().isAfter(nachDemAufruf));
    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket ohne Titel und Beschreibung WHEN toTicket wird THEN werden leere Strings statt null verwendet")
    void toTicket_verwendetLeereStrings_wennTitelUndBeschreibungFehlen() {

        // GIVEN:
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1005");

        // WHEN:
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        // THEN: Ich erwarte leere Strings statt null, damit spätere Anzeige-Logik
        // im Frontend nicht extra auf null prüfen muss.
        assertEquals("", result.getTitel());
        assertEquals("", result.getBeschreibung());
    }
}
