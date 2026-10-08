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
        String titel,
        // @Valid sorgt dafür, dass die @NotBlank-Prüfung in
        // ChecklistItemDto auch wirklich für jedes einzelne Item in
        // der Liste greift, nicht nur für die Liste als Ganzes.
        @NotEmpty(message = "Eine Checkliste braucht mindestens ein Item")
        @Valid
        List<ChecklistItemDto> items,
        LocalDateTime erstelltAm,
        LocalDateTime abgeschlossenAm
) {
}