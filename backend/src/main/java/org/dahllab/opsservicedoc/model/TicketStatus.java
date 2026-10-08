package org.dahllab.opsservicedoc.model;

// Enum für den Bearbeitungsstatus eines Tickets.
// Orientiert sich an den GLPI-Status-Codes, aber mit eigenen, sprechenden Namen,
// da wir nicht die rohen GLPI-Zahlen (1-6) direkt im eigenen Code verwenden wollen.
public enum TicketStatus {
    NEU,            // entspricht GLPI-Status 1 (INCOMING)
    IN_BEARBEITUNG, // entspricht GLPI-Status 2/3 (ASSIGNED/PLANNED)
    AUSSTEHEND,        // entspricht GLPI-Status 4 (WAITING)
    GELOEST,        // entspricht GLPI-Status 5 (SOLVED)
    GESCHLOSSEN     // entspricht GLPI-Status 6 (CLOSED)
}
