package org.dahllab.opsservicedoc.model;

// Status eines IPD-Dokuments. Bewusst nur zwei einfache Zustände, ohne
// Freigabe-Workflow (laut MVP-Scope meines Abschlussprojekts):
// ENTWURF direkt nach dem automatischen Erzeugen aus dem Ticket,
// ABGESCHLOSSEN sobald ich als Techniker alle Abschnitte geprüft/
// ergänzt habe und es an den Kunden rausgehen kann.
public enum IpdDocumentStatus {
    ENTWURF,
    ABGESCHLOSSEN
}
