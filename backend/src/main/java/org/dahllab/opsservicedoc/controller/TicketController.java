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

@Tag(name = "Tickets", description = "Create, retrieve, update and synchronize support tickets from GLPI")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Get all tickets")
    @ApiResponse(responseCode = "200", description = "List of all tickets (may be empty)")
    @GetMapping
    public List<TicketDto> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @Operation(summary = "Get a ticket by its ID")
    @ApiResponse(responseCode = "200", description = "Ticket found",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "404", description = "No ticket with this ID exists", content = @Content)
    @GetMapping("/{id}")
    public TicketDto getTicketById(@Parameter(description = "ID of the ticket") @PathVariable String id) {
        return ticketService.getTicketById(id);
    }

    @Operation(summary = "Create a new ticket manually")
    @ApiResponse(responseCode = "201", description = "Ticket was created",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "400", description = "Request body is invalid (e.g. titel is missing)", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto createTicket(@Valid @RequestBody TicketDto newTicket) {
        return ticketService.createTicket(newTicket);
    }

    @Operation(summary = "Fully update an existing ticket")
    @ApiResponse(responseCode = "200", description = "Ticket was updated",
            content = @Content(schema = @Schema(implementation = TicketDto.class)))
    @ApiResponse(responseCode = "404", description = "No ticket with this ID exists", content = @Content)
    @PutMapping("/{id}")
    public TicketDto updateTicket(
            @Parameter(description = "ID of the ticket to update") @PathVariable String id,
            @Valid @RequestBody TicketDto updatedTicket) {
        return ticketService.updateTicket(id, updatedTicket);
    }

    @Operation(
            summary = "Synchronize tickets from GLPI",
            description = "Imports tickets from the external GLPI system (read-only). Each call " +
                    "currently creates new tickets and does not yet update existing ones."
    )
    @ApiResponse(responseCode = "200", description = "List of imported tickets")
    @PostMapping("/sync-glpi")
    public List<TicketDto> syncFromGlpi() {
        return ticketService.syncFromGlpi();
    }
}
