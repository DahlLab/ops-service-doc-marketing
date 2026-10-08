package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.model.ScenarioType;
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
    void toTicket_copiesAllFields_whenGlpiTicketIsComplete() {

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
        assertEquals("Server Enterprise-01 Wartung", result.getTitle());
        assertEquals("Geplantes Patching ausserhalb der Betriebszeiten.", result.getDescription());
        assertEquals(TicketStatus.IN_PROGRESS, result.getStatus());
        assertEquals(ScenarioType.SERVER_MAINTENANCE, result.getScenarioType());
        assertEquals(LocalDateTime.of(2026, 9, 22, 9,15, 0), result.getCreatedAt());

    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket mit unbekanntem Status WHEN toTicket aufgerufen wird THEN wird NEW als Fallback verwendet")
    void toTicket_usesNewAsFallback_whenStatusIsUnknown() {

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
        assertEquals(TicketStatus.NEW, result.getStatus());

    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket ohne Status-Feld WHEN toTicket aufgerufen wird THEN wird NEW als Fallback verwendet")
    void toTicket_usesNewAsFallback_whenStatusFieldIsMissing() {

        // GIVEN: Ich lasse das status-Feld komplett weg, um zu prüfen, dass mein
        // Mapper auch bei fehlenden (nicht nur unbekannten) Werten nicht abstürzt.
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1003");
        glpiTicket.put("name", "Ticket ohne Status");

        // WHEN:
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        // THEN:
        assertEquals(TicketStatus.NEW, result.getStatus());
    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket mit unlesbarem Datum WHEN toTicket aufgerufen wird THEN wird das aktuelle Datum als Fallback verwendet")
    void toTicket_usesCurrentDateAsFallback_whenDateIsUnreadable() {

        // GIVEN: Ich simuliere ein kaputtes/unerwartetes Datumsformat.
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1004");
        glpiTicket.put("name", "Ticket mit kaputtem Datum");
        glpiTicket.put("date", "kein-gueltiges-datum");

        LocalDateTime beforeCall = LocalDateTime.now();

        // WHEN:
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        LocalDateTime afterCall = LocalDateTime.now();

        // THEN: Ich kann das exakte "jetzt" nicht vorhersagen, prüfe aber,
        // dass der Fallback-Zeitpunkt zwischen meinen beiden Messungen liegt,
        // das bestätigt, dass wirklich LocalDateTime.now() verwendet wurde.
        assertTrue(!result.getCreatedAt().isBefore(beforeCall)
                && !result.getCreatedAt().isAfter(afterCall));
    }

    @Test
    @DisplayName("GIVEN ein GLPI-Ticket ohne Titel und Beschreibung WHEN toTicket wird THEN werden leere Strings statt null verwendet")
    void toTicket_usesEmptyStrings_whenTitleAndDescriptionAreMissing() {

        // GIVEN:
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1005");

        // WHEN:
        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        // THEN: Ich erwarte leere Strings statt null, damit spätere Anzeige-Logik
        // im Frontend nicht extra auf null prüfen muss.
        assertEquals("", result.getTitle());
        assertEquals("", result.getDescription());
    }
}
