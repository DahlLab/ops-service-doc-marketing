// Diese Datei bildet 1:1 meine Backend-DTOs und -Enums als TypeScript-
// Typen ab (siehe TicketDto.java, TaskDto.java und die zugehörigen
// Enum-Klassen im Backend). Ich halte das bewusst in einer zentralen
// Datei statt verstreut in den einzelnen Seiten-Komponenten, damit ich
// bei einer Änderung im Backend nur an EINER Stelle im Frontend
// nachziehen muss (DRY). Ich ergänze diese Datei um weitere Typen,
// sobald ich die nächsten Seiten (Checklisten, IPD-Generator) baue.

// ---------------------------------------------------------------
// Ticket
// ---------------------------------------------------------------

// Entspricht exakt TicketStatus.java im Backend. Ich nutze einen
// String-Union-Type statt eines TypeScript-enums, weil das besser mit
// JSON harmoniert (Jackson serialisiert Enums als einfache Strings,
// kein zusätzliches Mapping nötig) und weil TypeScript mir bei einem
// Tippfehler (z.B. "GELOST" statt "GELOEST") sofort einen Fehler zeigt.
export type TicketStatus =
    | 'NEW'
    | 'IN_PROGRESS'
    | 'PENDING'
    | 'SOLVED'
    | 'CLOSED';

// Sprechende deutsche Labels für die Statuswerte, damit ich im UI nicht
// die rohen Enum-Namen (z.B. "IN_BEARBEITUNG") anzeigen muss. Als
// Record<TicketStatus, string> getippt, damit TypeScript mir einen
// Fehler zeigt, falls ich im Backend einen neuen Status ergänze, hier
// aber vergesse, ein Label dafür nachzutragen.
export const TICKET_STATUS_LABELS: Record<TicketStatus, string> = {
    NEW: 'Neu',
    IN_PROGRESS: 'In Bearbeitung',
    PENDING: 'Ausstehend',
    SOLVED: 'Gelöst',
    CLOSED: 'Geschlossen',
};

// Entspricht SzenarioTyp.java. Aktuell nur ein einziger Wert, weil laut
// Projektplanung SERVER_WARTUNG das erste und einzige Szenario für den
// Start ist (siehe Kommentar im Backend-Enum). Sobald im Backend ein
// weiterer Szenario-Typ ergänzt wird, muss ich hier UND in
// SZENARIO_TYP_LABELS nachziehen.
export type ScenarioType = 'SERVER_MAINTENANCE';

export const SCENARIO_TYPE_LABELS: Record<ScenarioType, string> = {
    SERVER_MAINTENANCE: 'Server-Wartung',
};

// Entspricht TicketDto.java (Java-Record) Feld für Feld, in derselben
// Reihenfolge, damit ich beim Vergleichen mit dem Backend nicht
// durcheinanderkomme.
export interface TicketDto {
    id: string;
    title: string;
    description: string;
    status: TicketStatus;
    technician: string;
    scenarioType: ScenarioType;
    // Jackson serialisiert LocalDateTime als ISO-8601-String
    // (z.B. "2026-10-04T18:27:00") - ich halte das im Frontend bewusst
    // als reinen String und wandle ihn erst beim Anzeigen mit
    // new Date(...) um, statt schon hier ein Date-Objekt zu bauen.
    createdAt: string;
}

// Für das Anlegen/Bearbeiten-Formular brauche ich weder die id (die
// vergibt MongoDB erst beim Speichern) noch erstelltAm (das setzt mein
// TicketService automatisch, siehe Backend-Kommentar dort). Mit Omit<>
// leite ich das direkt vom TicketDto ab, statt einen komplett eigenen,
// fast identischen Typ von Hand zu pflegen (DRY).
export type TicketFormData = Omit<TicketDto, 'id' | 'createdAt'>;

// ---------------------------------------------------------------
// Task
// ---------------------------------------------------------------

// Entspricht TaskStatus.java.
export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE';

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
    OPEN: 'Offen',
    IN_PROGRESS: 'In Bearbeitung',
    DONE: 'Erledigt',
};

// Entspricht TaskDto.java Feld für Feld.
export interface TaskDto {
    id: string;
    // Verweist auf die MongoDB-id des zugehörigen Tickets (nicht die
    // glpiTicketId), genau wie im Backend-Model kommentiert.
    ticketId: string;
    topic: string;
    nextSteps: string;
    // Wird von meinem TaskService beim Anlegen automatisch auf "jetzt"
    // gesetzt - ich zeige diesen Wert nur an, biete ihn aber nirgends
    // als editierbares Feld an (siehe TaskFormData unten).
    recordedAt: string;
    // LocalDate (nur Datum, keine Uhrzeit) - Jackson serialisiert das als
    // "YYYY-MM-DD"-String, was praktischerweise exakt dem Format
    // entspricht, das ein <input type="date"> erwartet. Kann null sein,
    // wenn noch kein Zieldatum gesetzt wurde.
    dueDate: string | null;
    // Bleibt null, bis der Task auf ERLEDIGT wechselt - wird dann
    // automatisch von meinem TaskService gesetzt (siehe
    // TaskService.updateTask-Kommentar im Backend).
    doneAt: string | null;
    status: TaskStatus;
}

// Nur die Felder, die der Nutzer im Formular tatsächlich selbst
// pflegt. erfasstAm und erledigtAm lasse ich bewusst außen vor, weil
// mein Backend die beiden serverseitig berechnet (siehe
// TaskService.createTask/updateTask) - würde ich sie hier mit
// reinnehmen, könnte ich aus Versehen einen falschen Wert mitschicken,
// der dann vom Service ohnehin überschrieben wird.
export type TaskFormData = Pick<TaskDto, 'ticketId' | 'topic' | 'nextSteps' | 'dueDate' | 'status'>;

// --- Ergänzung für src/api/types.ts: ans Ende der Datei anhängen ---

// Ein einzelnes Item innerhalb einer Checkliste. Die id ist nullable,
// weil beim Neuanlegen (im Formular) noch keine vergeben ist - mein
// Backend (ChecklistService.erzeugeItemsMitId) vergibt dann selbst
// eine UUID, falls id fehlt.
export interface ChecklistItemDto {
    id: string | null;
    description: string;
    done: boolean;
}

export interface ChecklistDto {
    id: string;
    ticketId: string;
    title: string;
    items: ChecklistItemDto[];
    createdAt: string;
    // Bleibt null, solange nicht alle Items erledigt sind - wird vom
    // Backend automatisch gesetzt (ChecklistService.setzeAbschlussdatumWennAlleErledigt).
    completedAt: string | null;
}

// Für das Formular lasse ich erstelltAm/abgeschlossenAm weg - beide
// werden serverseitig gepflegt, genau wie bei TaskFormData.
export type ChecklistFormData = Pick<ChecklistDto, 'ticketId' | 'title' | 'items'>;

export interface ChecklistTemplateDto {
    id: string;
    name: string;
    itemDescriptions: string[];
    // true nur bei den zehn vom ChecklistTemplateSeeder angelegten
    // Standard-Vorlagen - wird komplett vom Backend verwaltet
    // (ChecklistTemplateService), ich setze das selbst nie von Hand.
    // Eine Standard-Vorlage kann nicht gelöscht werden.
    builtIn: boolean;
}

// standard lasse ich hier zusätzlich zu id weg - das wird serverseitig
// gepflegt (siehe ChecklistTemplateDto), genau wie erstelltAm bei
// ChecklistFormData.
export type ChecklistTemplateFormData = Omit<ChecklistTemplateDto, 'id' | 'builtIn'>;

export type IpdDocumentStatus = 'DRAFT' | 'COMPLETED';
export const IPD_DOCUMENT_STATUS_LABELS: Record<IpdDocumentStatus, string> = {
    DRAFT: 'Entwurf',
    COMPLETED: 'Abgeschlossen',
};

// Feldreihenfolge entspricht IpdDocumentDto.java im Backend, damit ich
// beim Abgleich nicht durcheinanderkomme.
export interface IpdDocumentDto {
    id: string;
    ticketId: string;
    status: IpdDocumentStatus;
    title: string;
    technician: string;
    scenarioType: ScenarioType;
    customer: string | null;
    customerContact: string | null;
    period: string | null;
    initialSituation: string | null;
    requirements: string | null;
    infrastructureOverview: string | null;
    serversAndVms: string | null;
    network: string | null;
    rolesAndResponsibilities: string | null;
    backupPlan: string | null;
    securityConsiderations: string | null;
    // Wird vom Backend automatisch aus den erledigten Tasks des Tickets
    // zusammengebaut - im Formular zeige ich das nur read-only an.
    performedSteps: string | null;
    decisions: string | null;
    risksAndAssumptions: string | null;
    rollbackPlan: string | null;
    // Wird vom Backend automatisch aus den Checklisten des Tickets
    // ermittelt - ebenfalls nur read-only.
    qualityAssuranceCompleted: boolean;
    createdAt: string;
    updatedAt: string;
}

// Nur die Felder, die ich im Bearbeiten-Formular wirklich anbiete -
// ticketId/techniker/szenarioTyp/durchgefuehrteSchritte/
// qualitaetssicherungAbgeschlossen ignoriert IpdDocumentService.updateIpdDocument()
// ohnehin serverseitig, die muss ich also gar nicht erst im Request
// mitschicken (siehe handleSpeichern in IpdDocumentPage für den
// vollständigen DTO, den ich trotzdem ans Backend schicken muss).
export type IpdDocumentFormData = Pick<
    IpdDocumentDto,
    | 'title'
    | 'customer'
    | 'customerContact'
    | 'period'
    | 'initialSituation'
    | 'requirements'
    | 'infrastructureOverview'
    | 'serversAndVms'
    | 'network'
    | 'rolesAndResponsibilities'
    | 'backupPlan'
    | 'securityConsiderations'
    | 'decisions'
    | 'risksAndAssumptions'
    | 'rollbackPlan'
    | 'status'
>;