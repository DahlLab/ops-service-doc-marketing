import { currentLocale } from '../i18n';
import type { IpdDocumentStatus, TaskStatus, TicketStatus } from '../api/types';

export function formatDate(isoDate: string | null): string {
    if (!isoDate) return '–';
    return new Date(isoDate).toLocaleString(currentLocale(), { dateStyle: 'medium', timeStyle: 'short' });
}

export function formatDueDate(date: string | null): string {
    if (!date) return '–';
    return new Date(date).toLocaleDateString(currentLocale(), { dateStyle: 'medium' });
}

export function ticketStatusBadgeVariant(status: TicketStatus): string {
    switch (status) {
        case 'NEW':
            return 'secondary';
        case 'IN_PROGRESS':
            return 'primary';
        case 'PENDING':
            return 'warning';
        case 'SOLVED':
            return 'success';
        case 'CLOSED':
            return 'dark';
    }
}

export function taskStatusBadgeVariant(status: TaskStatus): string {
    switch (status) {
        case 'OPEN':
            return 'secondary';
        case 'IN_PROGRESS':
            return 'primary';
        case 'DONE':
            return 'success';
    }
}

export function ipdStatusBadgeVariant(status: IpdDocumentStatus): string {
    return status === 'COMPLETED' ? 'success' : 'secondary';
}
