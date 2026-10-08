package org.dahllab.opsservicedoc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record ChecklistDto(
        String id,
        String ticketId,
        @NotBlank(message = "Titel darf nicht leer sein")
        String title,

        @NotEmpty(message = "Eine Checkliste braucht mindestens ein Item")
        @Valid
        List<ChecklistItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
