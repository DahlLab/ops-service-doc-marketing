package org.dahllab.opsservicedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST-Controller für alles rund um Tickets.
// Nimmt HTTP-Anfragen entgegen und delegiert die eigentliche Arbeit
// an den TicketService, enthält selbst KEINE Business-Logik
// (Single Responsibility: Controller = Schnittstelle, Service = Logik).
@Tag(name = "Tickets", description = "Support-Tickets anlegen, abrufen, aktualisieren und aus GLPI synchronisieren")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // GET /api/tickets - liefert alle Tickets zurück.
    // Durch die SecurityConfig bereits als "authenticated()" geschützt,
    // hier also keine zusätzliche Prüfung nötig (DRY: Security-Regeln
    // zentral in einer Klasse, nicht in jedem Controller wiederholt).
    @Operation(summary = "Alle Tickets abrufen")
    @ApiResponse(responseCode = "200", description = "Liste aller Tickets (kann leer sein)")
    @GetMapping
    public List<TicketDto> getAllTickets() {
        return ticketService.getAllTickets();
    }

    // GET /api/tickets/{id} - liefert genau EIN Ticket anhand seiner ID.
    // Falls die ID nicht existiert, wirft der Service eine
    // NoSuchElementException, die der globale GlobalExceptionHandler
    // abfängt und sauber in 404 Not Found übersetzt (siehe
    // exception/GlobalExceptionHandler.java).
    @Operation(summary = "Ein Ticket anhand seiner ID abrufen")
    @ApiResponse(responseCode = "200", description = "Ticket gefunden",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein Ticket mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}")
    public TicketDto getTicketById(@Parameter(description = "ID des Tickets") @PathVariable String id) {
        return ticketService.getTicketById(id);
    }

    // POST /api/tickets - legt ein neues Ticket manuell an.
    // @Valid: löst die Bean-Validation-Prüfungen aus (siehe @NotBlank
    // auf titel im Ticket-Model).
    // @ResponseStatus(CREATED): gibt korrekt 201 statt dem Standard-200
    // zurück, wie es sich für einen erfolgreichen POST gehört
    // (REST-Konvention, wie bei meinen früheren Backends im Bootcamp).
    @Operation(summary = "Ein neues Ticket manuell anlegen")
    @ApiResponse(responseCode = "201", description = "Ticket wurde erstellt",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "400", description = "Request-Body ist ungültig (z.B. titel fehlt)", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto createTicket(@Valid @RequestBody TicketDto newTicket) {
        return ticketService.createTicket(newTicket);
    }

    // PUT /api/tickets/{id} - aktualisiert ein bestehendes Ticket vollständig
    // (z.B. um den Status zu ändern, wenn ein Techniker die Bearbeitung abschließt).
    // Anders als PATCH überschreibt PUT laut REST-Konvention alle Felder mit
    // den übergebenen Werten - der Aufrufer muss also das komplette Ticket
    // mitschicken, nicht nur das geänderte Feld.
    @Operation(summary = "Ein bestehendes Ticket vollständig aktualisieren")
    @ApiResponse(responseCode = "200", description = "Ticket wurde aktualisiert",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein Ticket mit dieser ID vorhanden", content = @Content)
    @PutMapping("/{id}")
    public TicketDto updateTicket(
            @Parameter(description = "ID des zu aktualisierenden Tickets") @PathVariable String id,
            @Valid @RequestBody TicketDto updatedTicket) {
        return ticketService.updateTicket(id, updatedTicket);
    }

    // POST /api/tickets/sync-glpi - stößt den manuellen Import aus GLPI an.
    // Bewusst als eigener Endpoint statt automatisch beim Start: so behalte
    // ich die Kontrolle, WANN der Sync passiert (relevant, da jeder Aufruf
    // aktuell neue Tickets anlegt statt zu aktualisieren, siehe Kommentar
    // in TicketService.syncFromGlpi()).
    @Operation(
            summary = "Tickets aus GLPI synchronisieren",
            description = "Importiert Tickets aus dem externen GLPI-System (nur lesend). Jeder Aufruf " +
                    "legt aktuell neue Tickets an, aktualisiert noch keine bestehenden."
    )
    @ApiResponse(responseCode = "200", description = "Liste der importierten Tickets")
    @PostMapping("/sync-glpi")
    public List<TicketDto> syncFromGlpi() {
        return ticketService.syncFromGlpi();
    }
}