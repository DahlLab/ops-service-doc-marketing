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

@Tag(name = "Tasks", description = "Manage tasks/work steps for a ticket (TaskPlanner area)")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Get all tasks",
            description = "Returns all tasks. If the optional parameter ticketId is provided, " +
                    "only the tasks for exactly that ticket are returned."
    )
    @ApiResponse(responseCode = "200", description = "List of tasks (may be empty)")
    @GetMapping
    public List<TaskDto> getAllTasks(
            @Parameter(description = "Optional ticket ID to filter the results")
            @RequestParam(required = false) String ticketId) {
        if (ticketId != null) {
            return taskService.getTasksByTicketId(ticketId);
        }
        return taskService.getAllTasks();
    }

    @Operation(summary = "Get a task by its ID")
    @ApiResponse(responseCode = "200", description = "Task found",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "404", description = "No task with this ID exists", content = @Content)
    @GetMapping("/{id}")
    public TaskDto getTaskById(@Parameter(description = "ID of the task") @PathVariable String id) {
        return taskService.getTaskById(id);
    }

    @Operation(summary = "Create a new task")
    @ApiResponse(responseCode = "201", description = "Task was created",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "400", description = "Request body is invalid (e.g. thema is missing)", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@Valid @RequestBody TaskDto taskDto) {
        return taskService.createTask(taskDto);
    }

    @Operation(summary = "Fully update an existing task")
    @ApiResponse(responseCode = "200", description = "Task was updated",
            content = @Content(schema = @Schema(implementation = TaskDto.class)))
    @ApiResponse(responseCode = "404", description = "No task with this ID exists", content = @Content)
    @PutMapping("/{id}")
    public TaskDto updateTask(
            @Parameter(description = "ID of the task to update") @PathVariable String id,
            @Valid @RequestBody TaskDto taskDto) {
        return taskService.updateTask(id, taskDto);
    }

    @Operation(summary = "Delete a task")
    @ApiResponse(responseCode = "204", description = "Task was deleted", content = @Content)
    @ApiResponse(responseCode = "404", description = "No task with this ID exists", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@Parameter(description = "ID of the task to delete") @PathVariable String id) {
        taskService.deleteTask(id);
    }
}
