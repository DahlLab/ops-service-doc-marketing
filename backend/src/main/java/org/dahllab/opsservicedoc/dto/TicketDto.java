package org.dahllab.opsservicedoc.dto;

import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.TicketStatus;

import java.time.LocalDateTime;

// DTO (Data Transfer Object) für Tickets.
// Wird zwischen Backend und Frontend über die REST-API ausgetauscht.
// Bewusst getrennt vom Ticket-Model (model-Package): so können ich später
// z.B. interne Felder ( wie glpiTicketId) weglassen oder Felder umbenennen,
// ohne die Datenbank-Struktur direkt zu beeinflussen (Singel Responsibility).
//
// record statt normaler Klasse: DTOs sind reine, unveränderliche Datencontainer
// ein Java-Record erzeugt automatisch Konstruktor, Getter, equals(), hashCode()
// und toString(), ohne das ich das per Hand oder mit Lombok schreiben muss (DRY & KISS).
public record TicketDto(
    String id,
    String title,
    String description,
    TicketStatus status,
    String technician,
    ScenarioType scenarioType,
    LocalDateTime createdAt
) {
    // Kein zusätzlicher Code nötig - der Record deckt alles ab, was ich
}   // aktuell brauche ( YAGNI: keine ungenutzen Felder/Methoden vorbauen).


