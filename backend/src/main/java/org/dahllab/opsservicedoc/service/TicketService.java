package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.dahllab.opsservicedoc.repository.TicketRepository;
import org.dahllab.opsservicedoc.util.GlpiTicketMapper;
import org.dahllab.opsservicedoc.util.TicketMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// @Service: markiert diese Klasse als Spring-verwaltete Business-Logik-Komponente.
// Enthält die eigentliche Anwendungslogik - der Controller soll nur
// HTTP-Anfragen entgegennehmen/weiterleiten, nicht selbst Logik enthalten
// (Single Responsibility Principle).
@Service
public class TicketService {

    // Zugriff auf die Datenbank über das Repository.
    private final TicketRepository ticketRepository;
    // Zugriff auf die GLPI-API, über den ich echte Tickets abrufe.
    private final GlpiClient glpiClient;

    // Konstruktor-Injection statt @Autowired auf den Feldern: macht die
    // Abhängigkeiten unveränderlich und explizit sichtbar, erleichtert
    // außerdem das Testen mit Mockito.
    public TicketService(TicketRepository ticketRepository, GlpiClient glpiClient) {
        this.ticketRepository = ticketRepository;
        this.glpiClient = glpiClient;
    }

    // Liefert alle Tickets als DTOs zurück.
    // Falls die Datenbank noch leer ist (z.B. beim allerersten Start),
    // lege ich einmalig Mock-Daten an - praktisch zum Testen ohne
    // eigene Testdaten von Hand einfügen zu müssen.
    public List<TicketDto> getAllTickets() {
        if (ticketRepository.count() == 0) {
            ticketRepository.saveAll(createMockTickets());
        }

        return ticketRepository.findAll()
                .stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    // Liefert ein einzelnes Ticket anhand seiner ID.
    // orElseThrow(): wirft eine NoSuchElementException, falls die ID
    // nicht existiert - einfache, eingebaute Java-Lösung statt einer
    // eigenen Exception-Klasse (KISS, solange kein spezielleres
    // Fehlerverhalten gebraucht wird).
    public TicketDto getTicketById(String id) {
        return ticketRepository.findById(id)
                .map(TicketMapper::toDto)
                .orElseThrow(() -> new NoSuchElementException("Ticket mit ID " + id + " nicht gefunden"));
    }

    // Legt ein neues Ticket an. Der eingehende TicketDto enthält noch
    // keine ID (die generiert MongoDB automatisch) und kein erstelltAm
    // (das setze ich hier zentral auf "jetzt") - der Aufrufer muss sich
    // also nicht selbst um diese technischen Details kümmern.
    public TicketDto createTicket(TicketDto newTicket) {
        Ticket ticket = new Ticket(
                null,              // MongoDB generiert die ID
                null,              // glpiTicketId: beim manuellen Anlegen noch unbekannt
                newTicket.title(),
                newTicket.description(),
                newTicket.status(),
                newTicket.technician(),
                newTicket.scenarioType(),
                LocalDateTime.now(ZoneId.systemDefault())
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketMapper.toDto(savedTicket);
    }

    // Aktualisiert ein bestehendes Ticket. Prüfe zuerst, ob die ID existiert
    // (sonst NoSuchElementException, wie schon bei getTicketById), und
    // überschreibe dann alle Felder außer der ID selbst - die ID bleibt
    // erhalten, damit ich dasselbe Dokument in MongoDB aktualisiere statt
    // versehentlich ein neues anzulegen.
    //
    // BEKANNTE EINSCHRÄNKUNG: glpiTicketId wird hier auf null gesetzt und
    // geht damit bei jedem manuellen Update verloren. Betrifft nur den
    // Fall, dass ein per Sync importiertes Ticket danach manuell über
    // PUT bearbeitet wird - für den jetzigen Projekt-Scope unkritisch.
    public TicketDto updateTicket(String id, TicketDto updatedTicket) {
        if (!ticketRepository.existsById(id)) {
            throw new NoSuchElementException("Ticket mit ID " + id + " nicht gefunden");
        }

        Ticket ticket = new Ticket(
                id,
                null,
                updatedTicket.title(),
                updatedTicket.description(),
                updatedTicket.status(),
                updatedTicket.technician(),
                updatedTicket.scenarioType(),
                updatedTicket.createdAt()
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketMapper.toDto(savedTicket);
    }

    // Holt alle Tickets von GLPI, wandle sie über den GlpiTicketMapper in
    // mein eigenes Ticket-Model um, und speichere sie in MongoDB.
    //
    // UPSERT-LOGIK: Für jedes GLPI-Ticket prüfe ich zuerst, ob bereits ein
    // Ticket mit derselben glpiTicketId existiert (anhand von
    // findByGlpiTicketId). Falls ja, aktualisiere ich das bestehende Ticket
    // (behalte seine MongoDB-ID), statt ein Duplikat anzulegen. Falls nein,
    // lege ich ein neues Ticket an. So kann ich den Sync beliebig oft
    // wiederholen, ohne dass sich die Ticketliste bei jedem Aufruf verdoppelt.
    public List<TicketDto> syncFromGlpi() {
        List<Map<String, Object>> glpiTickets = glpiClient.getAllGlpiTickets();

        List<Ticket> savedTickets = glpiTickets.stream()
                .map(this::upsertGlpiTicket)
                .toList();

        return savedTickets.stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    // Wandelt ein rohes GLPI-Ticket in mein Model um und speichert es -
    // entweder als Update eines bestehenden Tickets (gleiche glpiTicketId)
    // oder als komplett neues Ticket.
    private Ticket upsertGlpiTicket(Map<String, Object> glpiTicket) {
        Ticket newTicket = GlpiTicketMapper.toTicket(glpiTicket);

        return ticketRepository.findByGlpiTicketId(newTicket.getGlpiTicketId())
                .map(existingTicket -> {
                    // Bestehendes Ticket gefunden: MongoDB-ID des bestehenden
                    // Dokuments übernehmen, damit save() es AKTUALISIERT statt
                    // ein neues Dokument anzulegen.
                    newTicket.setId(existingTicket.getId());
                    return ticketRepository.save(newTicket);
                })
                .orElseGet(() -> ticketRepository.save(newTicket));
    }

    // Erzeugt ein paar Beispiel-Tickets mit Star-Trek-Testdaten,
    // passend zu meinen bisherigen Bootcamp-Projekten.
    // Bewusst als private Hilfsmethode innerhalb des Service gehalten,
    // da sie aktuell nur hier gebraucht wird (KISS: keine unnötige
    // eigene Klasse für einen einzigen Verwendungszweck).
    private List<Ticket> createMockTickets() {
        return List.of(
                new Ticket(null, "GLPI-1001", "Server Enterprise-01 Wartung",
                        "Geplantes Patching des vSphere-Clusters außerhalb der Betriebszeiten.",
                        TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now(ZoneId.systemDefault())),
                new Ticket(null, "GLPI-1002", "Backup-Check Enterprise-02",
                        "Wöchentliche Kontrolle der Backup-Jobs.",
                        TicketStatus.IN_PROGRESS, "N. Uhura", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now(ZoneId.systemDefault()))
        );
    }
}