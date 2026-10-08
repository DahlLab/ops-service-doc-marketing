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

@Tag(name = "Tickets", description = "Support-Tickets anlegen, abrufen, aktualisieren und aus GLPI synchronisieren")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Alle Tickets abrufen")
    @ApiResponse(responseCode = "200", description = "Liste aller Tickets (kann leer sein)")
    @GetMapping
    public List<TicketDto> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @Operation(summary = "Ein Ticket anhand seiner ID abrufen")
    @ApiResponse(responseCode = "200", description = "Ticket gefunden",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein Ticket mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}")
    public TicketDto getTicketById(@Parameter(description = "ID des Tickets") @PathVariable String id) {
        return ticketService.getTicketById(id);
    }

    @Operation(summary = "Ein neues Ticket manuell anlegen")
    @ApiResponse(responseCode = "201", description = "Ticket wurde erstellt",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "400", description = "Request-Body ist ungültig (z.B. titel fehlt)", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto createTicket(@Valid @RequestBody TicketDto newTicket) {
        return ticketService.createTicket(newTicket);
    }

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
