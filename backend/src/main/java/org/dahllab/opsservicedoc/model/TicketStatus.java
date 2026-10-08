package org.dahllab.opsservicedoc.model;

// Enum für den Bearbeitungsstatus eines Tickets.
// Orientiert sich an den GLPI-Status-Codes, aber mit eigenen, sprechenden Namen,
// da wir nicht die rohen GLPI-Zahlen (1-6) direkt im eigenen Code verwenden wollen.
public enum TicketStatus {
    NEW,            // entspricht GLPI-Status 1 (INCOMING)
    IN_PROGRESS, // entspricht GLPI-Status 2/3 (ASSIGNED/PLANNED)
    PENDING,        // entspricht GLPI-Status 4 (WAITING)
    SOLVED,        // entspricht GLPI-Status 5 (SOLVED)
    CLOSED     // entspricht GLPI-Status 6 (CLOSED)
}
