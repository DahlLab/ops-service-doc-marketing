export type TicketStatus =
    | 'NEW'
    | 'IN_PROGRESS'
    | 'PENDING'
    | 'SOLVED'
    | 'CLOSED';

export const TICKET_STATUS_LABELS: Record<TicketStatus, string> = {
    NEW: 'Neu',
    IN_PROGRESS: 'In Bearbeitung',
    PENDING: 'Ausstehend',
    SOLVED: 'Gelöst',
    CLOSED: 'Geschlossen',
};

export type ScenarioType = 'SERVER_MAINTENANCE';

export const SCENARIO_TYPE_LABELS: Record<ScenarioType, string> = {
    SERVER_MAINTENANCE: 'Server-Wartung',
};

export interface TicketDto {
    id: string;
    title: string;
    description: string;
    status: TicketStatus;
    technician: string;
    scenarioType: ScenarioType;

    createdAt: string;
}

export type TicketFormData = Omit<TicketDto, 'id' | 'createdAt'>;

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE';

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
    OPEN: 'Offen',
    IN_PROGRESS: 'In Bearbeitung',
    DONE: 'Erledigt',
};

export interface TaskDto {
    id: string;

    ticketId: string;
    topic: string;
    nextSteps: string;

    recordedAt: string;

    dueDate: string | null;

    doneAt: string | null;
    status: TaskStatus;
}

export type TaskFormData = Pick<TaskDto, 'ticketId' | 'topic' | 'nextSteps' | 'dueDate' | 'status'>;

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

    completedAt: string | null;
}

export type ChecklistFormData = Pick<ChecklistDto, 'ticketId' | 'title' | 'items'>;

export interface ChecklistTemplateDto {
    id: string;
    name: string;
    itemDescriptions: string[];

    builtIn: boolean;
}

export type ChecklistTemplateFormData = Omit<ChecklistTemplateDto, 'id' | 'builtIn'>;

export type IpdDocumentStatus = 'DRAFT' | 'COMPLETED';
export const IPD_DOCUMENT_STATUS_LABELS: Record<IpdDocumentStatus, string> = {
    DRAFT: 'Entwurf',
    COMPLETED: 'Abgeschlossen',
};

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

    performedSteps: string | null;
    decisions: string | null;
    risksAndAssumptions: string | null;
    rollbackPlan: string | null;

    qualityAssuranceCompleted: boolean;
    createdAt: string;
    updatedAt: string;
}

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
