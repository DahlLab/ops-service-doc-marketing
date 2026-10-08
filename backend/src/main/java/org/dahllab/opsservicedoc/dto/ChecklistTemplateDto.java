package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChecklistTemplateDto(
        String id,
        @NotBlank(message = "Name darf nicht leer sein")
        String name,
        @NotEmpty(message = "Eine Vorlage braucht mindestens einen Punkt")
        List<@NotBlank(message = "Ein Punkt darf nicht leer sein") String> itemDescriptions,

        Boolean builtIn
) {
}
