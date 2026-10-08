package org.dahllab.opsservicedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dahllab.opsservicedoc.dto.TaskDto;
import org.dahllab.opsservicedoc.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController: markiert diese Klasse als REST-Endpunkt, jede
// Rückgabe wird automatisch als JSON serialisiert.
// Enthält bewusst KEINE Business-Logik (die steckt im TaskService),
// der Controller nimmt nur HTTP-Anfragen entgegen, reicht sie weiter
// und gibt das Ergebnis zurück (Single Responsibility Principle).
@Tag(name = "Tasks", description = "Aufgaben/Arbeitsschritte zu einem Ticket verwalten (TaskPlanner-Bereich)")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    // Konstruktor-Injection statt @Autowired auf dem Feld, macht die
    // Abhängigkeit unveränderlich und explizit sichtbar.
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Optionaler Query-Parameter ticketId: ohne Parameter liefere ich alle
    // Tasks, mit Parameter nur die Tasks zu genau diesem Ticket, so
    // brauche ich keinen zweiten Endpunkt für den Filter.
    @Operation(
            summary = "Alle Tasks abrufen",
            description = "Liefert alle Tasks. Wird der optionale Parameter ticketId mitgegeben, " +
                    "werden nur die Tasks zu genau diesem Ticket zurückgegeben."
    )
    @ApiResponse(responseCode = "200", description = "Liste der Tasks (kann leer sein)")
    @GetMapping
    public List<TaskDto> getAllTasks(
            @Parameter(description = "Optionale Ticket-ID zum Filtern der Ergebnisse")
            @RequestParam(required = false) String ticketId) {
        if (ticketId != null) {
            return taskService.getTasksByTicketId(ticketId);
        }
        return taskService.getAllTasks();
    }

    // Liefert einen einzelnen Task anhand seiner ID. Falls die ID nicht
    // existiert, wirft der Service eine NoSuchElementException, die der
    // globale GlobalExceptionHandler abfängt und sauber in 404 Not Found
    // übersetzt (siehe exception/GlobalExceptionHandler.java).
    @Operation(summary = "Einen Task anhand seiner ID abrufen")
    @ApiResponse(responseCode = "200", description = "Task gefunden",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein Task mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}")
    public TaskDto getTaskById(@Parameter(description = "ID des Tasks") @PathVariable String id) {
        return taskService.getTaskById(id);
    }

    // Legt einen neuen Task an. @Valid löst die Validierung im TaskDto aus
    // (z.B. @NotBlank auf thema), bei ungültigen Daten antwortet Spring
    // automatisch mit 400 Bad Request, bevor die Methode überhaupt läuft.
    // @ResponseStatus(CREATED): erfolgreiches Anlegen liefert 201 statt
    // des Standard-200, wie es sich für POST gehört.
    @Operation(summary = "Einen neuen Task anlegen")
    @ApiResponse(responseCode = "201", description = "Task wurde erstellt",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "400", description = "Request-Body ist ungültig (z.B. thema fehlt)", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@Valid @RequestBody TaskDto taskDto) {
        return taskService.createTask(taskDto);
    }

    // Aktualisiert einen bestehenden Task komplett (alle Felder werden
    // überschrieben). Die ID kommt aus der URL, nicht aus dem Body,
    // so kann ich nicht versehentlich die ID eines anderen Tasks überschreiben.
    @Operation(summary = "Einen bestehenden Task vollständig aktualisieren")
    @ApiResponse(responseCode = "200", description = "Task wurde aktualisiert",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein Task mit dieser ID vorhanden", content = @Content)
    @PutMapping("/{id}")
    public TaskDto updateTask(
            @Parameter(description = "ID des zu aktualisierenden Tasks") @PathVariable String id,
            @Valid @RequestBody TaskDto taskDto) {
        return taskService.updateTask(id, taskDto);
    }

    // Löscht einen Task endgültig. @ResponseStatus(NO_CONTENT): bei
    // erfolgreichem Löschen gibt es keinen Response-Body, nur 204,
    // das ist der übliche REST-Standard für DELETE.
    @Operation(summary = "Einen Task löschen")
    @ApiResponse(responseCode = "204", description = "Task wurde gelöscht", content = @Content)
    @ApiResponse(responseCode = "404", description = "Kein Task mit dieser ID vorhanden", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@Parameter(description = "ID des zu löschenden Tasks") @PathVariable String id) {
        taskService.deleteTask(id);
    }
}