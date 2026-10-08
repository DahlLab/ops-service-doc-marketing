import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import { formatDate, formatDueDate, taskStatusBadgeVariant } from '../utils/formatting';
import {
    TASK_STATUS_LABELS,
    type TaskDto,
    type TaskFormData,
    type TaskStatus,
    type TicketDto,
} from '../api/types';

const EMPTY_FORM: TaskFormData = {
    ticketId: '',
    topic: '',
    nextSteps: '',
    dueDate: null,
    status: 'OPEN',
};

export function TaskPlannerPage() {
    const [tasks, setTasks] = useState<TaskDto[]>([]);

    const [tickets, setTickets] = useState<TicketDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const [ticketFilter, setTicketFilter] = useState('');

    const [modalOpen, setModalOpen] = useState(false);

    const [editedTask, setEditedTask] = useState<TaskDto | null>(null);
    const [form, setForm] = useState<TaskFormData>(EMPTY_FORM);

    async function loadTasks(ticketId: string) {
        try {
            const path = ticketId ? `/api/tasks?ticketId=${ticketId}` : '/api/tasks';
            const loadedTasks = await api.get<TaskDto[]>(path);
            setTasks(loadedTasks);
            setErrorMessage(null);
        } catch (error) {
            setErrorMessage('Tasks konnten nicht geladen werden.');
            console.error(error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        let aborted = false;
        api.get<TicketDto[]>('/api/tickets').then(setTickets).catch(console.error);
        api.get<TaskDto[]>('/api/tasks')
            .then((loadedTasks) => {
                if (aborted) return;
                setTasks(loadedTasks);
                setErrorMessage(null);
            })
            .catch((error) => {
                if (aborted) return;
                setErrorMessage('Tasks konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setLoading(false);
            });
        return () => {
            aborted = true;
        };
    }, []);

    function handleFilterChange(ticketId: string) {
        setTicketFilter(ticketId);
        setLoading(true);
        void loadTasks(ticketId);
    }

    function ticketTitle(ticketId: string): string {
        return tickets.find((t) => t.id === ticketId)?.title ?? '(unbekanntes Ticket)';
    }

    function handleNewTask(ticketIdPreselection?: string) {
        setEditedTask(null);
        setForm({
            ...EMPTY_FORM,
            ticketId: ticketIdPreselection || ticketFilter || tickets[0]?.id || '',
        });
        setModalOpen(true);
    }

    function handleEdit(task: TaskDto) {
        setEditedTask(task);
        setForm({
            ticketId: task.ticketId,
            topic: task.topic,
            nextSteps: task.nextSteps,
            dueDate: task.dueDate,
            status: task.status,
        });
        setModalOpen(true);
    }

    async function handleSave() {
        setSaving(true);
        setErrorMessage(null);
        try {
            if (editedTask) {
                await api.put<TaskDto>(`/api/tasks/${editedTask.id}`, form);
            } else {
                await api.post<TaskDto>('/api/tasks', form);
            }
            setModalOpen(false);

            await loadTasks(ticketFilter);
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setErrorMessage('Bitte Ticket und Thema ausfüllen.');
            } else {
                setErrorMessage('Task konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(task: TaskDto) {
        if (!window.confirm(`Task "${task.topic}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/tasks/${task.id}`);
            await loadTasks(ticketFilter);
        } catch (error) {
            setErrorMessage('Task konnte nicht gelöscht werden.');
            console.error(error);
        }
    }

    if (loading && tasks.length === 0) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        );
    }

    return (
        <div className="py-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h1 className="mb-0">TaskPlanner</h1>

                <Button variant="primary" onClick={() => handleNewTask()} disabled={tickets.length === 0}>
                    Neuer Task
                </Button>
            </div>

            {tickets.length === 0 && (
                <Alert variant="info">
                    Es existiert noch kein Ticket - lege zuerst ein Ticket an, bevor du Tasks erfassen kannst.
                </Alert>
            )}

            <Form.Group className="mb-3" style={{ maxWidth: 320 }}>
                <Form.Label>Nach Ticket filtern</Form.Label>
                <Form.Select value={ticketFilter} onChange={(e) => handleFilterChange(e.target.value)}>
                    <option value="">Alle Tickets</option>
                    {tickets.map((ticket) => (
                        <option key={ticket.id} value={ticket.id}>
                            {ticket.title}
                        </option>
                    ))}
                </Form.Select>
            </Form.Group>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}

            <HudPanel title="Aufgaben">
<Table hover responsive className="hud-table">
                <thead>
                <tr>
                    <th>Thema</th>
                    <th>Ticket</th>
                    <th>Nächste Schritte</th>
                    <th>Zieldatum</th>
                    <th>Status</th>
                    <th>Erfasst am</th>
                    <th>Erledigt am</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                {tasks.length === 0 ? (
                    <tr>
                        <td colSpan={8} className="text-center text-muted py-4">
                            <div className="mb-2">
                                {ticketFilter
                                    ? `Für "${ticketTitle(ticketFilter)}" sind noch keine Tasks erfasst.`
                                    : 'Keine Tasks vorhanden.'}
                            </div>

                            <Button
                                variant="outline-primary"
                                size="sm"
                                onClick={() => handleNewTask(ticketFilter)}
                                disabled={tickets.length === 0}
                            >
                                + Task {ticketFilter ? 'für dieses Ticket' : ''} anlegen
                            </Button>
                        </td>
                    </tr>
                ) : (
                    tasks.map((task) => (
                        <tr key={task.id}>
                            <td>{task.topic}</td>
                            <td>{ticketTitle(task.ticketId)}</td>
                            <td>{task.nextSteps}</td>
                            <td>{formatDueDate(task.dueDate)}</td>
                            <td>
                                <Badge bg={taskStatusBadgeVariant(task.status)}>{TASK_STATUS_LABELS[task.status]}</Badge>
                            </td>
                            <td>{formatDate(task.recordedAt)}</td>
                            <td>{formatDate(task.doneAt)}</td>
                            <td>

                                <div className="d-flex gap-2">
                                    <Button variant="outline-secondary" size="sm" onClick={() => handleEdit(task)}>
                                        Bearbeiten
                                    </Button>
                                    <Button variant="outline-danger" size="sm" onClick={() => handleDelete(task)}>
                                        Löschen
                                    </Button>
                                </div>
                            </td>
                        </tr>
                    ))
                )}
                </tbody>
            </Table>
</HudPanel>

            <Modal show={modalOpen} onHide={() => setModalOpen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{editedTask ? 'Task bearbeiten' : 'Neuer Task'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Ticket</Form.Label>
                            <Form.Select
                                value={form.ticketId}
                                onChange={(e) => setForm({ ...form, ticketId: e.target.value })}
                            >
                                {tickets.map((ticket) => (
                                    <option key={ticket.id} value={ticket.id}>
                                        {ticket.title}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Thema</Form.Label>
                            <Form.Control
                                type="text"
                                value={form.topic}
                                onChange={(e) => setForm({ ...form, topic: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Nächste Schritte</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.nextSteps}
                                onChange={(e) => setForm({ ...form, nextSteps: e.target.value })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Zieldatum</Form.Label>
                            <Form.Control
                                type="date"

                                value={form.dueDate ?? ''}

                                onChange={(e) => setForm({ ...form, dueDate: e.target.value || null })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Status</Form.Label>
                            <Form.Select
                                value={form.status}
                                onChange={(e) => setForm({ ...form, status: e.target.value as TaskStatus })}
                            >
                                {Object.entries(TASK_STATUS_LABELS).map(([value, label]) => (
                                    <option key={value} value={value}>
                                        {label}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setModalOpen(false)}>
                        Abbrechen
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSave}
                        disabled={saving || !form.topic || !form.ticketId}
                    >
                        {saving ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}
