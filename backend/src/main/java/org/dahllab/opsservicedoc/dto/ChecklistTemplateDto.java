package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChecklistTemplateDto(
        String id,
        @NotBlank(message = "Name must not be empty")
        String name,
        @NotEmpty(message = "A template needs at least one item")
        List<@NotBlank(message = "An item must not be empty") String> itemDescriptions,

        Boolean builtIn
) {
}
