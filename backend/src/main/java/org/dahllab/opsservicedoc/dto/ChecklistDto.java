package org.dahllab.opsservicedoc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record ChecklistDto(
        String id,
        String ticketId,
        @NotBlank(message = "Title must not be empty")
        String title,

        @NotEmpty(message = "A checklist needs at least one item")
        @Valid
        List<ChecklistItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
