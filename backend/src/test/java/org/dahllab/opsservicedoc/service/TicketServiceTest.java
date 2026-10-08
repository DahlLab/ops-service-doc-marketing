package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.SzenarioTyp;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.dahllab.opsservicedoc.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @ExtendWith (MockitoExtension.class) aktiviert Mockito-Unterstützung für JUnit 5,
// OHNE einen kompletten Spring-Kontext zu starten, dadurch läuft dieser Test
// deutlich schneller als ein @SpringBootTest (KISS: nur so vile testen wie nötig).
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    // @Mock: erzeugt ein simuliertes TicketRepository, das ich selbst
    // mit Rückgabewerten "füttern" kann, ohne eine echte MongoDB zu brauchen.
    @Mock
    private TicketRepository ticketRepository;

    // @InjectMocks: erzeugt eine echte TicketService-Instanz und injiziert
    // automatisch das obige Mock-Repository hinein (über den Konstruktor).
    @InjectMocks
    private TicketService ticketService;

    @Test
    @DisplayName("GIVEN eine leere Datenbank WHEN getAllTickets aufgerufen wird THEN werden Mock-Tickets angelegt und zurückgegeben")
    void getAllTickets_erzeugtMockDaten_wennDatenbankLeerIst() {

        // GIVEN: Ich simuliere eine leere Datenbank: count() liefert 0,
        // und findAll() gibt (nach dem simulierten Speichern) zwei Beispiel-Tickets zurück.
        Ticket ticket1 = new Ticket("1", "GLPI-1001", "Server-Wartung", "Beschreibung",
                TicketStatus.NEU, "M.Scott", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());
        Ticket ticket2 = new Ticket("2", "GLPI-1002", "Backup-Check", "Beschreibung",
                TicketStatus.IN_BEARBEITUNG, "N. Uhura", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());

        when(ticketRepository.count()).thenReturn(0L);
        when(ticketRepository.findAll()).thenReturn(List.of(ticket1, ticket2));

        // WHEN: Ich rufe die zu testende Methode auf.
        List<TicketDto> result = ticketService.getAllTickets();

        // THEN: Ich prüfe zwei Dinge:
        // 1. Es kommen genau 2 Tickets zurück (als DTOs, nicht als rohe Ticket-Objekte)
        // 2. saveAll() wurde tatsächlich aufgerufen, das die Datenbank zwar leer war
        // (verify prüft, ob eine bestimmte Methode auf dem Mock aufgerufen wurde)
        assertEquals(2, result.size());
        assertEquals("Server-Wartung", result.get(0).titel());
        verify(ticketRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("GIVEN eine bereits befüllte Datenbank WHEN getAllTickets aufgerufen wird THEN werden KEINE neuen Mock-Daten angelegt")
    void getAllTickets_erzeugtKeineMockDaten_wennDatenbankSchonBefuelltIst() {

        // GIVEN: Die Datenbank enthält bereits ein Ticket (count() > 0).
        Ticket vorhandenesTicket = new Ticket("1", "GLPI-1001", "Server-Wartung", "Beschreibung",
                TicketStatus.NEU, "M.Scott", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());

        when(ticketRepository.count()).thenReturn(1L);
        when(ticketRepository.findAll()).thenReturn(List.of(vorhandenesTicket));

        // WHEN
        List<TicketDto> result = ticketService.getAllTickets();

        // THEN: Es wird genau 1 Ticket zurückgegeben, UND saveAll() darf NICHT
        // aufgerufen worden sein, da schon Daten vorhanden waren
        // (verhindert, dass bei jedem Aufruf erneut Mock-Daten dazukommen).
        assertEquals(1, result.size());
        verify(ticketRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("GIVEN eine existierende ID WHEN getTicketById aufgerufen wird THEN wird das passende Ticket zurückgegeben")
    void getTicketById_gibtTicket_wennIdExistiert() {

        // GIVEN:
        Ticket ticket = new Ticket("1", "GLPI-1001", "Server-Wartung", "Beschreibung",
                TicketStatus.NEU, "M. Scott", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());
        when(ticketRepository.findById("1")).thenReturn(java.util.Optional.of(ticket));

        // WHEN:
        TicketDto result = ticketService.getTicketById("1");

        // THEN:
        assertEquals("Server-Wartung", result.titel());
    }

    @Test
    @DisplayName("GIVEN eine nicht existierende ID WHEN getTicketById aufgerufen wird THEN wird eine NoSuchElementException geworfen")
    void getTicketById_wirftException_wennIdNichtExistiert() {

        // GIVEN:
        when(ticketRepository.findById("unbekannt")).thenReturn(java.util.Optional.empty());

        // WHEN + THEN:
        // assertThrows prüft in einem Schritt, dass die Methode tatsächlich
        // die erwartete Exception wirft, statt normal zurückzukehren.
        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> ticketService.getTicketById("unbekannt")
        );
    }

    @Test
    @DisplayName("GIVEN ein neues Ticket WHEN createTicket aufgerufen wird THEN wird es gespeichert und als DTO zurückgegeben")
    void createTicket_speichertUndGibtTicketZurueck() {

        // GIVEN:
        // Das eingehende DTO (noch ohne ID, wie es vom Frontend käme).
        TicketDto neuesTicketDto = new TicketDto(null, "Neues Ticket", "Beschreibung",
                TicketStatus.NEU, "M. Scott", SzenarioTyp.SERVER_WARTUNG, null);

        // Das Repository simuliert das Speichern: es bekommt ein Ticket OHNE ID
        // übergeben und gibt eines MIT generierter ID zurück (wie MongoDB es tun würde).
        Ticket gespeichertesTicket = new Ticket("1", null, "Neues Ticket", "Beschreibung",
                TicketStatus.NEU, "M. Scott", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class))).thenReturn(gespeichertesTicket);

        // WHEN:
        TicketDto result = ticketService.createTicket(neuesTicketDto);

        // THEN:
        // Die zurückgegebene ID stammt aus dem simulierten Speichervorgang,
        // und der Titel wurde korrekt übernommen.
        assertEquals("1", result.id());
        assertEquals("Neues Ticket", result.titel());
    }

    @Test
    @DisplayName("GIVEN eine existierende ID WHEN updateTicket aufgerufen wird THEN wird das Ticket aktualisiert")
    void updateTicket_aktualisiertTicket_wennIdExistiert() {

        // GIVEN:
        TicketDto aktualisierteDaten = new TicketDto("1", "Geänderter Titel", "Beschreibung",
                TicketStatus.GELOEST, "M. Scott", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());

        when(ticketRepository.existsById("1")).thenReturn(true);

        Ticket gespeichertesTicket = new Ticket("1", null, "Geänderter Titel", "Beschreibung",
                TicketStatus.GELOEST, "M. Scott", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class))).thenReturn(gespeichertesTicket);

        // WHEN:
        TicketDto result = ticketService.updateTicket("1", aktualisierteDaten);

        // THEN:
        assertEquals("Geänderter Titel", result.titel());
        assertEquals(TicketStatus.GELOEST, result.status());
    }

    @Test
    @DisplayName("GIVEN eine nicht existierende ID WHEN updateTicket aufgerufen wird THEN wird eine NoSuchElementException geworfen")
    void updateTicket_wirftException_wennIdNichtExistiert() {

        // GIVEN:
        when(ticketRepository.existsById("unbekannt")).thenReturn(false);

        TicketDto beliebigeDaten = new TicketDto("unbekannt", "Titel", "Beschreibung",
                TicketStatus.NEU, "M. Scott", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());

        // WHEN + THEN:
        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> ticketService.updateTicket("unbekannt", beliebigeDaten)
        );
    }

    // Neues Mock-Feld für den GLPI-Client (zusätzlich zum bestehenden ticketRepository-Mock).
    @Mock
    private GlpiClient glpiClient;

    @Test
    @DisplayName("GIVEN ein neues GLPI-Ticket WHEN syncFromGlpi aufgerufen wird THEN wird es neu angelegt")
    void syncFromGlpi_legtNeuesTicketAn_wennNochNichtVorhanden() {

        // GIVEN:
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", 2001);
        glpiTicket.put("name", "GLPI Ticket");
        glpiTicket.put("status", 1);

        when(glpiClient.getAllGlpiTickets()).thenReturn(List.of(glpiTicket));
        // Kein bestehendes Ticket mit dieser glpiTicketId gefunden.
        when(ticketRepository.findByGlpiTicketId("2001")).thenReturn(java.util.Optional.empty());

        Ticket gespeichertesTicket = new Ticket("1", "2001", "GLPI Ticket", "",
                TicketStatus.NEU, "Nicht zugewiesen", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenReturn(gespeichertesTicket);

        // WHEN:
        List<TicketDto> result = ticketService.syncFromGlpi();

        // THEN:
        assertEquals(1, result.size());
        assertEquals("GLPI Ticket", result.get(0).titel());
    }

    @Test
    @DisplayName("GIVEN ein bereits importiertes GLPI-Ticket WHEN syncFromGlpi erneut aufgerufen wird THEN wird das bestehende Ticket aktualisiert statt dupliziert")
    void syncFromGlpi_aktualisiertBestehendesTicket_wennGlpiTicketIdSchonExistiert() {

        // GIVEN:
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", 2001);
        glpiTicket.put("name", "Geänderter Titel");
        glpiTicket.put("status", 5); // jetzt GELOEST statt NEU

        when(glpiClient.getAllGlpiTickets()).thenReturn(List.of(glpiTicket));

        // Es existiert bereits ein Ticket mit dieser glpiTicketId (aus einem früheren Sync).
        Ticket bestehendesTicket = new Ticket("bestehende-mongo-id", "2001", "Alter Titel", "",
                TicketStatus.NEU, "Nicht zugewiesen", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());
        when(ticketRepository.findByGlpiTicketId("2001")).thenReturn(java.util.Optional.of(bestehendesTicket));

        Ticket aktualisiertesTicket = new Ticket("bestehende-mongo-id", "2001", "Geänderter Titel", "",
                TicketStatus.GELOEST, "Nicht zugewiesen", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenReturn(aktualisiertesTicket);

        // WHEN:
        List<TicketDto> result = ticketService.syncFromGlpi();

        // THEN:
        // Genau EIN Ticket im Ergebnis (kein Duplikat), mit den aktualisierten Werten.
        assertEquals(1, result.size());
        assertEquals("Geänderter Titel", result.get(0).titel());
        assertEquals(TicketStatus.GELOEST, result.get(0).status());
    }

}
