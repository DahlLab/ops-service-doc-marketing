import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, Badge, Button, Form, Modal, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import i18n from '../i18n';
import { formatDate, formatDueDate, taskStatusBadgeVariant } from '../utils/formatting';
import {
    TASK_STATUSES,
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
    const { t } = useTranslation('tasks');
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
            setErrorMessage(t('errors.load'));
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
                setErrorMessage(i18n.t('tasks:errors.load'));
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
        return tickets.find((ticket) => ticket.id === ticketId)?.title ?? t('unknownTicket');
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
                setErrorMessage(t('errors.validation'));
            } else {
                setErrorMessage(t('errors.save'));
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(task: TaskDto) {
        if (!window.confirm(t('confirmDelete', { topic: task.topic }))) {
            return;
        }
        try {
            await api.delete(`/api/tasks/${task.id}`);
            await loadTasks(ticketFilter);
        } catch (error) {
            setErrorMessage(t('errors.delete'));
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
                <h1 className="mb-0">{t('title')}</h1>

                <Button variant="primary" onClick={() => handleNewTask()} disabled={tickets.length === 0}>
                    {t('newTask')}
                </Button>
            </div>

            {tickets.length === 0 && (
                <Alert variant="info">
                    {t('noTicketsHint')}
                </Alert>
            )}

            <Form.Group className="mb-3" style={{ maxWidth: 320 }}>
                <Form.Label>{t('filter.label')}</Form.Label>
                <Form.Select value={ticketFilter} onChange={(e) => handleFilterChange(e.target.value)}>
                    <option value="">{t('filter.all')}</option>
                    {tickets.map((ticket) => (
                        <option key={ticket.id} value={ticket.id}>
                            {ticket.title}
                        </option>
                    ))}
                </Form.Select>
            </Form.Group>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}

            <HudPanel title={t('panelTitle')}>
<Table hover responsive className="hud-table">
                <thead>
                <tr>
                    <th>{t('table.topic')}</th>
                    <th>{t('table.ticket')}</th>
                    <th>{t('table.nextSteps')}</th>
                    <th>{t('table.dueDate')}</th>
                    <th>{t('table.status')}</th>
                    <th>{t('table.recordedAt')}</th>
                    <th>{t('table.doneAt')}</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                {tasks.length === 0 ? (
                    <tr>
                        <td colSpan={8} className="text-center text-muted py-4">
                            <div className="mb-2">
                                {ticketFilter
                                    ? t('empty.forTicket', { ticket: ticketTitle(ticketFilter) })
                                    : t('empty.none')}
                            </div>

                            <Button
                                variant="outline-primary"
                                size="sm"
                                onClick={() => handleNewTask(ticketFilter)}
                                disabled={tickets.length === 0}
                            >
                                {ticketFilter ? t('empty.createForTicket') : t('empty.create')}
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
                                <Badge bg={taskStatusBadgeVariant(task.status)}>{t(`status.task.${task.status}`, { ns: 'common' })}</Badge>
                            </td>
                            <td>{formatDate(task.recordedAt)}</td>
                            <td>{formatDate(task.doneAt)}</td>
                            <td>

                                <div className="d-flex gap-2">
                                    <Button variant="outline-secondary" size="sm" onClick={() => handleEdit(task)}>
                                        {t('actions.edit')}
                                    </Button>
                                    <Button variant="outline-danger" size="sm" onClick={() => handleDelete(task)}>
                                        {t('actions.delete')}
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
                    <Modal.Title>{editedTask ? t('editTask') : t('newTask')}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.ticket')}</Form.Label>
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
                            <Form.Label>{t('form.topic')}</Form.Label>
                            <Form.Control
                                type="text"
                                value={form.topic}
                                onChange={(e) => setForm({ ...form, topic: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.nextSteps')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.nextSteps}
                                onChange={(e) => setForm({ ...form, nextSteps: e.target.value })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.dueDate')}</Form.Label>
                            <Form.Control
                                type="date"

                                value={form.dueDate ?? ''}

                                onChange={(e) => setForm({ ...form, dueDate: e.target.value || null })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.status')}</Form.Label>
                            <Form.Select
                                value={form.status}
                                onChange={(e) => setForm({ ...form, status: e.target.value as TaskStatus })}
                            >
                                {TASK_STATUSES.map((value) => (
                                    <option key={value} value={value}>
                                        {t(`status.task.${value}`, { ns: 'common' })}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setModalOpen(false)}>
                        {t('actions.cancel')}
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSave}
                        disabled={saving || !form.topic || !form.ticketId}
                    >
                        {saving ? t('actions.saving') : t('actions.save')}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}
