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

@Tag(name = "Tasks", description = "Aufgaben/Arbeitsschritte zu einem Ticket verwalten (TaskPlanner-Bereich)")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

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

    @Operation(summary = "Einen Task anhand seiner ID abrufen")
    @ApiResponse(responseCode = "200", description = "Task gefunden",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein Task mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}")
    public TaskDto getTaskById(@Parameter(description = "ID des Tasks") @PathVariable String id) {
        return taskService.getTaskById(id);
    }

    @Operation(summary = "Einen neuen Task anlegen")
    @ApiResponse(responseCode = "201", description = "Task wurde erstellt",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "400", description = "Request-Body ist ungültig (z.B. thema fehlt)", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@Valid @RequestBody TaskDto taskDto) {
        return taskService.createTask(taskDto);
    }

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

    @Operation(summary = "Einen Task löschen")
    @ApiResponse(responseCode = "204", description = "Task wurde gelöscht", content = @Content)
    @ApiResponse(responseCode = "404", description = "Kein Task mit dieser ID vorhanden", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@Parameter(description = "ID des zu löschenden Tasks") @PathVariable String id) {
        taskService.deleteTask(id);
    }
}
