package org.dahllab.opsservicedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dahllab.opsservicedoc.dto.ChecklistTemplateDto;
import org.dahllab.opsservicedoc.service.ChecklistTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Checklist templates", description = "Manage reusable templates for checklists")
@RestController
@RequestMapping("/api/checklist-templates")
public class ChecklistTemplateController {
    private final ChecklistTemplateService checklistTemplateService;

    public ChecklistTemplateController(ChecklistTemplateService checklistTemplateService) {
        this.checklistTemplateService = checklistTemplateService;
    }

    @Operation(
            summary = "Get all checklist templates",
            description = "Returns all templates, e.g. so the frontend can display a selection list."
    )
    @ApiResponse(responseCode = "200", description = "List of templates (may be empty)")
    @GetMapping
    public List<ChecklistTemplateDto> getAllTemplates() {
        return checklistTemplateService.getAllTemplates();
    }

    @Operation(summary = "Get a checklist template by its ID")
    @ApiResponse(responseCode = "200", description = "Template found",
            content = @Content(schema = @Schema(implementation = ChecklistTemplateDto.class)))
    @ApiResponse(responseCode = "404", description = "No template with this ID exists", content = @Content)
    @GetMapping("/{id}")
    public ChecklistTemplateDto getTemplateById(@Parameter(description = "ID of the template") @PathVariable String id) {
        return checklistTemplateService.getTemplateById(id);
    }

    @Operation(summary = "Create a new checklist template")
    @ApiResponse(responseCode = "201", description = "Template was created",
            content = @Content(schema = @Schema(implementation = ChecklistTemplateDto.class)))
    @ApiResponse(responseCode = "400", description = "Request body is invalid", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistTemplateDto createTemplate(@Valid @RequestBody ChecklistTemplateDto templateDto) {
        return checklistTemplateService.createTemplate(templateDto);
    }

    @Operation(summary = "Update a checklist template")
    @ApiResponse(responseCode = "200", description = "Template was updated",
            content = @Content(schema = @Schema(implementation = ChecklistTemplateDto.class)))
    @ApiResponse(responseCode = "404", description = "No template with this ID exists", content = @Content)
    @PutMapping("/{id}")
    public ChecklistTemplateDto updateTemplate(
            @Parameter(description = "ID of the template to update") @PathVariable String id,
            @Valid @RequestBody ChecklistTemplateDto templateDto) {
        return checklistTemplateService.updateTemplate(id, templateDto);
    }

    @Operation(
            summary = "Delete a checklist template",
            description = "Existing checklists that were already created from this template remain " +
                    "unaffected, since their items are embedded and no longer linked to the template."
    )
    @ApiResponse(responseCode = "204", description = "Template was deleted", content = @Content)
    @ApiResponse(responseCode = "404", description = "No template with this ID exists", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@Parameter(description = "ID of the template to delete") @PathVariable String id) {
        checklistTemplateService.deleteTemplate(id);
    }
}
