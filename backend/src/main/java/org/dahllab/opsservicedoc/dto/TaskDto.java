package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;
import org.dahllab.opsservicedoc.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskDto(
    String id,
    String ticketId,
    @NotBlank(message = "Thema darf nicht leer sein")
    String topic,
    String nextSteps,
    LocalDateTime recordedAt,
    LocalDate dueDate,
    LocalDateTime doneAt,
    TaskStatus status
) {
}
