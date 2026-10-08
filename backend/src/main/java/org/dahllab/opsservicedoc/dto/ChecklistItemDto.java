package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;

public record ChecklistItemDto(
        String id,
        @NotBlank(message = "Beschreibung darf nicht leer sein")
        String description,
        boolean done
) {
}
