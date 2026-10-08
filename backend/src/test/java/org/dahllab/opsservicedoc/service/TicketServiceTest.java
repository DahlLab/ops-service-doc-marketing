package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.ScenarioType;
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
    @DisplayName("GIVEN an empty database WHEN getAllTickets is called THEN mock tickets are created and returned")
    void getAllTickets_createsMockData_whenDatabaseIsEmpty() {

        // GIVEN: Ich simuliere eine leere Datenbank: count() liefert 0,
        // und findAll() gibt (nach dem simulierten Speichern) zwei Beispiel-Tickets zurück.
        Ticket ticket1 = new Ticket("1", "GLPI-1001", "Server-Wartung", "Beschreibung",
                TicketStatus.NEW, "M.Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        Ticket ticket2 = new Ticket("2", "GLPI-1002", "Backup-Check", "Beschreibung",
                TicketStatus.IN_PROGRESS, "N. Uhura", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketRepository.count()).thenReturn(0L);
        when(ticketRepository.findAll()).thenReturn(List.of(ticket1, ticket2));

        // WHEN: Ich rufe die zu testende Methode auf.
        List<TicketDto> result = ticketService.getAllTickets();

        // THEN: Ich prüfe zwei Dinge:
        // 1. Es kommen genau 2 Tickets zurück (als DTOs, nicht als rohe Ticket-Objekte)
        // 2. saveAll() wurde tatsächlich aufgerufen, das die Datenbank zwar leer war
        // (verify prüft, ob eine bestimmte Methode auf dem Mock aufgerufen wurde)
        assertEquals(2, result.size());
        assertEquals("Server-Wartung", result.get(0).title());
        verify(ticketRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("GIVEN an already filled database WHEN getAllTickets is called THEN NO new mock data is created")
    void getAllTickets_createsNoMockData_whenDatabaseIsAlreadyFilled() {

        // GIVEN: Die Datenbank enthält bereits ein Ticket (count() > 0).
        Ticket existingTicket = new Ticket("1", "GLPI-1001", "Server-Wartung", "Beschreibung",
                TicketStatus.NEW, "M.Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketRepository.count()).thenReturn(1L);
        when(ticketRepository.findAll()).thenReturn(List.of(existingTicket));

        // WHEN
        List<TicketDto> result = ticketService.getAllTickets();

        // THEN: Es wird genau 1 Ticket zurückgegeben, UND saveAll() darf NICHT
        // aufgerufen worden sein, da schon Daten vorhanden waren
        // (verhindert, dass bei jedem Aufruf erneut Mock-Daten dazukommen).
        assertEquals(1, result.size());
        verify(ticketRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("GIVEN an existing ID WHEN getTicketById is called THEN the matching ticket is returned")
    void getTicketById_returnsTicket_whenIdExists() {

        // GIVEN:
        Ticket ticket = new Ticket("1", "GLPI-1001", "Server-Wartung", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findById("1")).thenReturn(java.util.Optional.of(ticket));

        // WHEN:
        TicketDto result = ticketService.getTicketById("1");

        // THEN:
        assertEquals("Server-Wartung", result.title());
    }

    @Test
    @DisplayName("GIVEN a non-existing ID WHEN getTicketById is called THEN a NoSuchElementException is thrown")
    void getTicketById_throwsException_whenIdDoesNotExist() {

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
    @DisplayName("GIVEN a new ticket WHEN createTicket is called THEN it is saved and returned as DTO")
    void createTicket_savesAndReturnsTicket() {

        // GIVEN:
        // Das eingehende DTO (noch ohne ID, wie es vom Frontend käme).
        TicketDto newTicketDto = new TicketDto(null, "Neues Ticket", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, null);

        // Das Repository simuliert das Speichern: es bekommt ein Ticket OHNE ID
        // übergeben und gibt eines MIT generierter ID zurück (wie MongoDB es tun würde).
        Ticket savedTicket = new Ticket("1", null, "Neues Ticket", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class))).thenReturn(savedTicket);

        // WHEN:
        TicketDto result = ticketService.createTicket(newTicketDto);

        // THEN:
        // Die zurückgegebene ID stammt aus dem simulierten Speichervorgang,
        // und der Titel wurde korrekt übernommen.
        assertEquals("1", result.id());
        assertEquals("Neues Ticket", result.title());
    }

    @Test
    @DisplayName("GIVEN an existing ID WHEN updateTicket is called THEN the ticket is updated")
    void updateTicket_updatesTicket_whenIdExists() {

        // GIVEN:
        TicketDto updatedData = new TicketDto("1", "Geänderter Titel", "Beschreibung",
                TicketStatus.SOLVED, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketRepository.existsById("1")).thenReturn(true);

        Ticket savedTicket = new Ticket("1", null, "Geänderter Titel", "Beschreibung",
                TicketStatus.SOLVED, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class))).thenReturn(savedTicket);

        // WHEN:
        TicketDto result = ticketService.updateTicket("1", updatedData);

        // THEN:
        assertEquals("Geänderter Titel", result.title());
        assertEquals(TicketStatus.SOLVED, result.status());
    }

    @Test
    @DisplayName("GIVEN a non-existing ID WHEN updateTicket is called THEN a NoSuchElementException is thrown")
    void updateTicket_throwsException_whenIdDoesNotExist() {

        // GIVEN:
        when(ticketRepository.existsById("unbekannt")).thenReturn(false);

        TicketDto anyData = new TicketDto("unbekannt", "Titel", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        // WHEN + THEN:
        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> ticketService.updateTicket("unbekannt", anyData)
        );
    }

    // Neues Mock-Feld für den GLPI-Client (zusätzlich zum bestehenden ticketRepository-Mock).
    @Mock
    private GlpiClient glpiClient;

    @Test
    @DisplayName("GIVEN a new GLPI ticket WHEN syncFromGlpi is called THEN it is created")
    void syncFromGlpi_createsNewTicket_whenNotYetPresent() {

        // GIVEN:
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", 2001);
        glpiTicket.put("name", "GLPI Ticket");
        glpiTicket.put("status", 1);

        when(glpiClient.getAllGlpiTickets()).thenReturn(List.of(glpiTicket));
        // Kein bestehendes Ticket mit dieser glpiTicketId gefunden.
        when(ticketRepository.findByGlpiTicketId("2001")).thenReturn(java.util.Optional.empty());

        Ticket savedTicket = new Ticket("1", "2001", "GLPI Ticket", "",
                TicketStatus.NEW, "Nicht zugewiesen", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenReturn(savedTicket);

        // WHEN:
        List<TicketDto> result = ticketService.syncFromGlpi();

        // THEN:
        assertEquals(1, result.size());
        assertEquals("GLPI Ticket", result.get(0).title());
    }

    @Test
    @DisplayName("GIVEN an already imported GLPI ticket WHEN syncFromGlpi is called again THEN the existing ticket is updated instead of duplicated")
    void syncFromGlpi_updatesExistingTicket_whenGlpiTicketIdAlreadyExists() {

        // GIVEN:
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", 2001);
        glpiTicket.put("name", "Geänderter Titel");
        glpiTicket.put("status", 5); // jetzt GELOEST statt NEU

        when(glpiClient.getAllGlpiTickets()).thenReturn(List.of(glpiTicket));

        // Es existiert bereits ein Ticket mit dieser glpiTicketId (aus einem früheren Sync).
        Ticket existingTicket = new Ticket("bestehende-mongo-id", "2001", "Alter Titel", "",
                TicketStatus.NEW, "Nicht zugewiesen", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findByGlpiTicketId("2001")).thenReturn(java.util.Optional.of(existingTicket));

        Ticket updatedTicket = new Ticket("bestehende-mongo-id", "2001", "Geänderter Titel", "",
                TicketStatus.SOLVED, "Nicht zugewiesen", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenReturn(updatedTicket);

        // WHEN:
        List<TicketDto> result = ticketService.syncFromGlpi();

        // THEN:
        // Genau EIN Ticket im Ergebnis (kein Duplikat), mit den aktualisierten Werten.
        assertEquals(1, result.size());
        assertEquals("Geänderter Titel", result.get(0).title());
        assertEquals(TicketStatus.SOLVED, result.get(0).status());
    }

}
