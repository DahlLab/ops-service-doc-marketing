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

@Tag(name = "Checklists", description = "Manage checklists for a ticket, including creation from a template")
@RestController
@RequestMapping("/api/checklists")
public class ChecklistController {
    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @Operation(
            summary = "Get all checklists",
            description = "Returns all checklists. If the optional parameter ticketId is provided, " +
                    "only the checklists for exactly that ticket are returned."
    )
    @ApiResponse(responseCode = "200", description = "List of checklists (may be empty)")
    @GetMapping
    public List<ChecklistDto> getAllChecklists(
            @Parameter(description = "Optional ticket ID to filter the results")
            @RequestParam(required = false) String ticketId) {
        if (ticketId != null) {
            return checklistService.getChecklistsByTicketId(ticketId);
        }
        return checklistService.getAllChecklists();
    }

    @Operation(summary = "Get a checklist by its ID")
    @ApiResponse(responseCode = "200", description = "Checklist found",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "404", description = "No checklist with this ID exists", content = @Content)
    @GetMapping("/{id}")
    public ChecklistDto getChecklistById(@Parameter(description = "ID of the checklist") @PathVariable String id) {
        return checklistService.getChecklistById(id);
    }

    @Operation(summary = "Create a new checklist manually")
    @ApiResponse(responseCode = "201", description = "Checklist was created",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "400", description = "Request body is invalid", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistDto createChecklist(@Valid @RequestBody ChecklistDto checklistDto) {
        return checklistService.createChecklist(checklistDto);
    }

    @Operation(
            summary = "Create a checklist from a template",
            description = "Copies the items of an existing ChecklistTemplate into a new checklist " +
                    "for the given ticket, instead of sending the items manually."
    )
    @ApiResponse(responseCode = "201", description = "Checklist was created from the template",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "404", description = "Ticket or template not found", content = @Content)
    @PostMapping("/from-template")
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistDto createChecklistFromTemplate(@Valid @RequestBody CreateChecklistFromTemplateRequest request) {
        return checklistService.createChecklistFromTemplate(request.ticketId(), request.templateId());
    }

    @Operation(summary = "Update a checklist", description = "Updates the title and items, e.g. to tick off individual points.")
    @ApiResponse(responseCode = "200", description = "Checklist was updated",
            content = @Content(schema = @Schema(implementation = ChecklistDto.class)))
    @ApiResponse(responseCode = "404", description = "No checklist with this ID exists", content = @Content)
    @PutMapping("/{id}")
    public ChecklistDto updateChecklist(
            @Parameter(description = "ID of the checklist to update") @PathVariable String id,
            @Valid @RequestBody ChecklistDto checklistDto) {
        return checklistService.updateChecklist(id, checklistDto);
    }

    @Operation(summary = "Delete a checklist")
    @ApiResponse(responseCode = "204", description = "Checklist was deleted", content = @Content)
    @ApiResponse(responseCode = "404", description = "No checklist with this ID exists", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChecklist(@Parameter(description = "ID of the checklist to delete") @PathVariable String id) {
        checklistService.deleteChecklist(id);
    }
}
