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
        // Nur lesend relevant - wird vom Service gesetzt
        // (createTemplate erzwingt immer false, updateTemplate behält
        // den bestehenden Wert bei) und von einem Request-Body IMMER
        // ignoriert, damit niemand über die API eine eigene Vorlage
        // nachträglich zur unlöschbaren Standard-Vorlage machen kann
        // oder umgekehrt. Ich nehme bewusst den Wrapper-Typ Boolean statt
        // des primitiven boolean: Fehlt das Feld im JSON (mein Frontend
        // schickt es beim Anlegen/Bearbeiten gar nicht mit), würde Jackson
        // bei einem primitiven boolean mit "Cannot map null into type
        // boolean" abbrechen und die Anfrage mit 400 ablehnen. Mit Boolean
        // ist das Feld dann einfach null, und der Service ignoriert es ohnehin.
        Boolean builtIn
) {
}