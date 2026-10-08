package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;

// DTO für ein einzelnes Checklisten-Item. Die id ist hier bewusst
// nullable: beim Anlegen einer neuen Checkliste kann sie fehlen (der
// Service vergibt dann selbst eine neue UUID), beim Aktualisieren
// schicke ich sie mit, damit der Service weiß, welches Item gemeint ist.
public record ChecklistItemDto(
        String id,
        @NotBlank(message = "Beschreibung darf nicht leer sein")
        String beschreibung,
        boolean erledigt
) {
}