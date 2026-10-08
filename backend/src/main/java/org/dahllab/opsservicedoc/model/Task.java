package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

// @Document: MongoDB-Dokument in der Collection "tasks".
// Ein Task ist meine Aufgaben-/Notiz-Zeile zu einem Ticket, angelehnt  an
// die Excel-Tabelle, die ich bisher parallel zum TIcketsystem geführt habe
// (Erfassungsdatum, Thema, TIcket-Referenz, nächste Schritte, Zieldatum,
// Erledigungsdatum, Status).
@Document(collection = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    private String id;

    // Referenz auf die MongoDB_id des zugehörigen Tickets (nicht die
    // glpi-TicketId), damit ich sowohl manuell angelegte als auch per
    // GLPI-Sync importierte Tickets eindeutig referenzieren kann, so
    // weiß ich bei jedem Task, woran ich gerade arbeite
    private String ticketId;

    private String thema;

    private String naechsteSchritte;

    // Wird beim Anlegen automatisch im Service gesetzt, analog zu
    // Ticket.erstelltAm, der Aufrufer muss sich darum nicht kümmern.
    private LocalDateTime erfasstAm;

    private LocalDate zieldatum;

    // Bleibt null, solange der Task nicht erledigt ist. Wird im Service
    // automatisch gesetzt, sobald der Status auf ERLEDIGT wechselt,
    // ich muss das Datum also nicht manuell pflegen
    private LocalDateTime erledigtAm;

    private TaskStatus status;
}
