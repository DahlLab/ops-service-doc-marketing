package org.dahllab.opsservicedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dahllab.opsservicedoc.dto.ChecklistDto;
import org.dahllab.opsservicedoc.dto.CreateChecklistFromTemplateRequest;
import org.dahllab.opsservicedoc.service.ChecklistService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Checklisten", description = "Checklisten zu einem Ticket verwalten, inkl. Erzeugung aus einer Vorlage")
@RestController
@RequestMapping("/api/checklists")
public class ChecklistController {
    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @Operation(
            summary = "Alle Checklisten abrufen",
            description = "Liefert alle Checklisten. Wird der optionale Parameter ticketId mitgegeben, " +
                    "werden nur die Checklisten zu genau diesem Ticket zurückgegeben."
    )
    @ApiResponse(responseCode = "200", description = "Liste der Checklisten (kann leer sein)")
    @GetMapping
    public List<ChecklistDto> getAllChecklists(
            @Parameter(description = "Optionale Ticket-ID zum Filtern der Ergebnisse")
            @RequestParam(required = false) String ticketId) {
        if (ticketId != null) {
            return checklistService.getChecklistsByTicketId(ticketId);
        }
        return checklistService.getAllChecklists();
    }

    @Operation(summary = "Eine Checkliste anhand ihrer ID abrufen")
    @ApiResponse(responseCode = "200", description = "Checkliste gefunden",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "404", description = "Keine Checkliste mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}")
    public ChecklistDto getChecklistById(@Parameter(description = "ID der Checkliste") @PathVariable String id) {
        return checklistService.getChecklistById(id);
    }

    @Operation(summary = "Eine neue Checkliste manuell anlegen")
    @ApiResponse(responseCode = "201", description = "Checkliste wurde erstellt",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "400", description = "Request-Body ist ungültig", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistDto createChecklist(@Valid @RequestBody ChecklistDto checklistDto) {
        return checklistService.createChecklist(checklistDto);
    }

    @Operation(
            summary = "Eine Checkliste aus einer Vorlage erzeugen",
            description = "Übernimmt die Items einer bestehenden ChecklistTemplate in eine neue Checkliste " +
                    "für das angegebene Ticket, statt die Items manuell mitzuschicken."
    )
    @ApiResponse(responseCode = "201", description = "Checkliste wurde aus der Vorlage erstellt",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "404", description = "Ticket oder Vorlage nicht gefunden", content = @Content)
    @PostMapping("/from-template")
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistDto createChecklistFromTemplate(@Valid @RequestBody CreateChecklistFromTemplateRequest request) {
        return checklistService.createChecklistFromTemplate(request.ticketId(), request.templateId());
    }

    @Operation(summary = "Eine Checkliste aktualisieren", description = "Aktualisiert Titel und Items, z.B. um einzelne Punkte abzuhaken.")
    @ApiResponse(responseCode = "200", description = "Checkliste wurde aktualisiert",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "404", description = "Keine Checkliste mit dieser ID vorhanden", content = @Content)
    @PutMapping("/{id}")
    public ChecklistDto updateChecklist(
            @Parameter(description = "ID der zu aktualisierenden Checkliste") @PathVariable String id,
            @Valid @RequestBody ChecklistDto checklistDto) {
        return checklistService.updateChecklist(id, checklistDto);
    }

    @Operation(summary = "Eine Checkliste löschen")
    @ApiResponse(responseCode = "204", description = "Checkliste wurde gelöscht", content = @Content)
    @ApiResponse(responseCode = "404", description = "Keine Checkliste mit dieser ID vorhanden", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChecklist(@Parameter(description = "ID der zu löschenden Checkliste") @PathVariable String id) {
        checklistService.deleteChecklist(id);
    }
}
