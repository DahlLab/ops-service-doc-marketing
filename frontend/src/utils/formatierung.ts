import type { IpdDocumentStatus, TaskStatus, TicketStatus } from '../api/types';

// Gemeinsame Anzeige-Hilfsfunktionen für alle Seiten. Ich hatte
// formatiereDatum und die Status-Badge-Funktionen vorher in jeder
// Seite einzeln als Kopie stehen (TicketsPage, TaskPlannerPage,
// IpdDocumentPage, IpdGeneratorPage, ChecklistenPage) - SonarQube hat
// das als Duplikat bemängelt, und außerdem mussten die Funktionen
// laut Sonar ohnehin aus den Komponenten heraus, weil sie weder State
// noch Props brauchen. Jetzt gibt es jede Funktion genau einmal.

// Formatiert ein LocalDateTime vom Backend (z.B. "2026-10-04T18:27:00")
// in deutsches Datumsformat mit Uhrzeit. null wird als "–" angezeigt
// (z.B. solange ein Task noch nicht erledigt ist) - die Seiten, die nie
// null bekommen, sind davon nicht betroffen.
export function formatiereDatum(isoDatum: string | null): string {
    if (!isoDatum) return '–';
    return new Date(isoDatum).toLocaleString('de-DE', { dateStyle: 'medium', timeStyle: 'short' });
}

// Eigene Funktion für das Zieldatum (LocalDate statt LocalDateTime),
// weil ich hier nur das Datum ohne Uhrzeit anzeigen will - sonst
// würde new Date("2026-10-10") durch die Zeitzonen-Interpretation
// des Browsers manchmal den Vortag anzeigen, toLocaleDateString ohne
// timeStyle vermeidet diese Verwirrung in der Anzeige.
export function formatiereZieldatum(datum: string | null): string {
    if (!datum) return '–';
    return new Date(datum).toLocaleDateString('de-DE', { dateStyle: 'medium' });
}

// Ordnet jedem Ticket-Status eine Bootstrap-Badge-Farbe zu, damit man
// den Bearbeitungsstand eines Tickets auf einen Blick erkennt, ohne den
// Text lesen zu müssen (z.B. grün = fertig, grau = noch nicht begonnen).
export function ticketStatusBadgeVariante(status: TicketStatus): string {
    switch (status) {
        case 'NEU':
            return 'secondary';
        case 'IN_BEARBEITUNG':
            return 'primary';
        case 'AUSSTEHEND':
            return 'warning';
        case 'GELOEST':
            return 'success';
        case 'GESCHLOSSEN':
            return 'dark';
    }
}

// Grün = erledigt, Blau = in Arbeit, Grau = noch offen - auf einen
// Blick erkennbar, ohne den Text lesen zu müssen.
export function taskStatusBadgeVariante(status: TaskStatus): string {
    switch (status) {
        case 'OFFEN':
            return 'secondary';
        case 'IN_BEARBEITUNG':
            return 'primary';
        case 'ERLEDIGT':
            return 'success';
    }
}

// Grün = abgeschlossen, Grau = noch im Entwurf.
export function ipdStatusBadgeVariante(status: IpdDocumentStatus): string {
    return status === 'ABGESCHLOSSEN' ? 'success' : 'secondary';
}
