import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, Badge, Button, Form, Modal, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import i18n from '../i18n';
import { formatDate, ticketStatusBadgeVariant } from '../utils/formatting';
import {
    SCENARIO_TYPES,
    TICKET_STATUSES,
    type TicketDto,
    type TicketFormData,
    type TicketStatus,
    type ScenarioType,
} from '../api/types';

const EMPTY_FORM: TicketFormData = {
    title: '',
    description: '',
    status: 'NEW',
    technician: '',
    scenarioType: 'SERVER_MAINTENANCE',
};

export function TicketsPage() {
    const { t } = useTranslation('tickets');
    const [tickets, setTickets] = useState<TicketDto[]>([]);

    const [loading, setLoading] = useState(true);

    const [syncing, setSyncing] = useState(false);
    const [saving, setSaving] = useState(false);

    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const [modalOpen, setModalOpen] = useState(false);

    const [editedTicket, setEditedTicket] = useState<TicketDto | null>(null);

    const [form, setForm] = useState<TicketFormData>(EMPTY_FORM);

    async function loadTickets() {
        try {
            const loadedTickets = await api.get<TicketDto[]>('/api/tickets');
            setTickets(loadedTickets);

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
        api.get<TicketDto[]>('/api/tickets')
            .then((loadedTickets) => {
                if (aborted) return;
                setTickets(loadedTickets);
                setErrorMessage(null);
            })
            .catch((error) => {
                if (aborted) return;
                setErrorMessage(i18n.t('tickets:errors.load'));
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setLoading(false);
            });
        return () => {
            aborted = true;
        };
    }, []);

    async function handleGlpiSync() {
        setSyncing(true);
        setErrorMessage(null);
        try {
            await api.post<TicketDto[]>('/api/tickets/sync-glpi');
            await loadTickets();
        } catch (error) {
            setErrorMessage(t('errors.sync'));
            console.error(error);
        } finally {
            setSyncing(false);
        }
    }

    function handleNewTicket() {
        setEditedTicket(null);
        setForm(EMPTY_FORM);
        setModalOpen(true);
    }

    function handleEdit(ticket: TicketDto) {
        setEditedTicket(ticket);
        setForm({
            title: ticket.title,
            description: ticket.description,
            status: ticket.status,
            technician: ticket.technician,
            scenarioType: ticket.scenarioType,
        });
        setModalOpen(true);
    }

    async function handleSave() {
        setSaving(true);
        setErrorMessage(null);
        try {
            if (editedTicket) {
                await api.put<TicketDto>(`/api/tickets/${editedTicket.id}`, form);
            } else {
                await api.post<TicketDto>('/api/tickets', form);
            }
            setModalOpen(false);

            await loadTickets();
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

    if (loading) {
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
                <div className="d-flex gap-2">
                    <Button variant="outline-primary" onClick={handleGlpiSync} disabled={syncing}>
                        {syncing ? t('sync.running') : t('sync.idle')}
                    </Button>
                    <Button variant="primary" onClick={handleNewTicket}>
                        {t('newTicket')}
                    </Button>
                </div>
            </div>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}

            {tickets.length === 0 ? (
                <p className="text-muted">{t('empty')}</p>
            ) : (
                <HudPanel title={t('panelTitle')}>
<Table hover responsive className="hud-table">
                    <thead>
                    <tr>
                        <th>{t('table.title')}</th>
                        <th>{t('table.technician')}</th>
                        <th>{t('table.scenario')}</th>
                        <th>{t('table.status')}</th>
                        <th>{t('table.createdAt')}</th>

                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    {tickets.map((ticket) => (
                        <tr key={ticket.id}>
                            <td>{ticket.title}</td>
                            <td>{ticket.technician}</td>
                            <td>{t(`scenario.${ticket.scenarioType}`)}</td>
                            <td>
                                <Badge bg={ticketStatusBadgeVariant(ticket.status)}>
                                    {t(`status.ticket.${ticket.status}`, { ns: 'common' })}
                                </Badge>
                            </td>
                            <td>{formatDate(ticket.createdAt)}</td>
                            <td>
                                <Button variant="outline-secondary" size="sm" onClick={() => handleEdit(ticket)}>
                                    {t('actions.edit')}
                                </Button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </Table>
</HudPanel>
            )}

            <Modal show={modalOpen} onHide={() => setModalOpen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{editedTicket ? t('editTicket') : t('newTicket')}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.title')}</Form.Label>
                            <Form.Control
                                type="text"
                                value={form.title}

                                onChange={(e) => setForm({ ...form, title: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.description')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.description}
                                onChange={(e) => setForm({ ...form, description: e.target.value })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.technician')}</Form.Label>
                            <Form.Control
                                type="text"
                                value={form.technician}
                                onChange={(e) => setForm({ ...form, technician: e.target.value })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.status')}</Form.Label>
                            <Form.Select
                                value={form.status}

                                onChange={(e) => setForm({ ...form, status: e.target.value as TicketStatus })}
                            >

                                {TICKET_STATUSES.map((value) => (
                                    <option key={value} value={value}>
                                        {t(`status.ticket.${value}`, { ns: 'common' })}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('form.scenario')}</Form.Label>
                            <Form.Select
                                value={form.scenarioType}
                                onChange={(e) => setForm({ ...form, scenarioType: e.target.value as ScenarioType })}
                            >
                                {SCENARIO_TYPES.map((value) => (
                                    <option key={value} value={value}>
                                        {t(`scenario.${value}`)}
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

                    <Button variant="primary" onClick={handleSave} disabled={saving || !form.title}>
                        {saving ? t('actions.saving') : t('actions.save')}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}
