package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;

public record ChecklistItemDto(
        String id,
        @NotBlank(message = "Description must not be empty")
        String description,
        boolean done
) {
}
