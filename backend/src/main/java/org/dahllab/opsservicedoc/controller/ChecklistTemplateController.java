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

@Tag(name = "Checklisten-Vorlagen", description = "Wiederverwendbare Vorlagen für Checklisten verwalten")
@RestController
@RequestMapping("/api/checklist-templates")
public class ChecklistTemplateController {

    private final ChecklistTemplateService checklistTemplateService;

    public ChecklistTemplateController(ChecklistTemplateService checklistTemplateService) {
        this.checklistTemplateService = checklistTemplateService;
    }

    // GET /api/checklist-templates - liefert alle Vorlagen, z.B. damit
    // das Frontend eine Auswahlliste anzeigen kann, aus der ich beim
    // Anlegen einer Checkliste eine Vorlage auswähle.
    @Operation(
            summary = "Alle Checklisten-Vorlagen abrufen",
            description = "Liefert alle Vorlagen, z.B. damit das Frontend eine Auswahlliste anzeigen kann."
    )
    @ApiResponse(responseCode = "200", description = "Liste der Vorlagen (kann leer sein)")
    @GetMapping
    public List<ChecklistTemplateDto> getAllTemplates() {
        return checklistTemplateService.getAllTemplates();
    }

    // GET /api/checklist-templates/{id} - liefert genau eine Vorlage
    // anhand ihrer ID. Existiert die ID nicht, wirft der Service eine
    // NoSuchElementException, die der GlobalExceptionHandler in 404
    // übersetzt (gleiches Muster wie bei Ticket/Task/Checklist).
    @Operation(summary = "Eine Checklisten-Vorlage anhand ihrer ID abrufen")
    @ApiResponse(responseCode = "200", description = "Vorlage gefunden",
            content = @Content(schema = @Schema(implementation = ChecklistTemplateDto.class)))
    @ApiResponse(responseCode = "404", description = "Keine Vorlage mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}")
    public ChecklistTemplateDto getTemplateById(@Parameter(description = "ID der Vorlage") @PathVariable String id) {
        return checklistTemplateService.getTemplateById(id);
    }

    // POST /api/checklist-templates - legt eine neue Vorlage an, die
    // ich später über ChecklistService.createChecklistFromTemplate()
    // wiederverwenden kann.
    @Operation(summary = "Eine neue Checklisten-Vorlage anlegen")
    @ApiResponse(responseCode = "201", description = "Vorlage wurde erstellt",
            content = @Content(schema = @Schema(implementation = ChecklistTemplateDto.class)))
    @ApiResponse(responseCode = "400", description = "Request-Body ist ungültig", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistTemplateDto createTemplate(@Valid @RequestBody ChecklistTemplateDto templateDto) {
        return checklistTemplateService.createTemplate(templateDto);
    }

    // PUT /api/checklist-templates/{id} - aktualisiert Name und
    // Item-Beschreibungen einer bestehenden Vorlage.
    @Operation(summary = "Eine Checklisten-Vorlage aktualisieren")
    @ApiResponse(responseCode = "200", description = "Vorlage wurde aktualisiert",
            content = @Content(schema = @Schema(implementation = ChecklistTemplateDto.class)))
    @ApiResponse(responseCode = "404", description = "Keine Vorlage mit dieser ID vorhanden", content = @Content)
    @PutMapping("/{id}")
    public ChecklistTemplateDto updateTemplate(
            @Parameter(description = "ID der zu aktualisierenden Vorlage") @PathVariable String id,
            @Valid @RequestBody ChecklistTemplateDto templateDto) {
        return checklistTemplateService.updateTemplate(id, templateDto);
    }

    // DELETE /api/checklist-templates/{id} - löscht eine Vorlage.
    // Bestehende Checklisten, die schon aus dieser Vorlage erzeugt
    // wurden, bleiben davon unberührt, da die Items dort eingebettet
    // und nicht mehr mit der Vorlage verknüpft sind.
    @Operation(
            summary = "Eine Checklisten-Vorlage löschen",
            description = "Bestehende Checklisten, die schon aus dieser Vorlage erzeugt wurden, bleiben " +
                    "davon unberührt, da die Items dort eingebettet und nicht mehr mit der Vorlage verknüpft sind."
    )
    @ApiResponse(responseCode = "204", description = "Vorlage wurde gelöscht", content = @Content)
    @ApiResponse(responseCode = "404", description = "Keine Vorlage mit dieser ID vorhanden", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@Parameter(description = "ID der zu löschenden Vorlage") @PathVariable String id) {
        checklistTemplateService.deleteTemplate(id);
    }
}