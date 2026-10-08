import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import { formatDate, ticketStatusBadgeVariante } from '../utils/formatting';
import {
    SCENARIO_TYPE_LABELS,
    TICKET_STATUS_LABELS,
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
            setErrorMessage('Tickets konnten nicht geladen werden.');

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
                setErrorMessage('Tickets konnten nicht geladen werden.');
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
            setErrorMessage('GLPI-Synchronisierung ist fehlgeschlagen.');
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
                setErrorMessage('Bitte alle Pflichtfelder ausfüllen (mindestens der Titel).');
            } else {
                setErrorMessage('Ticket konnte nicht gespeichert werden.');
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
                <h1 className="mb-0">Tickets</h1>
                <div className="d-flex gap-2">
                    <Button variant="outline-primary" onClick={handleGlpiSync} disabled={syncing}>
                        {syncing ? 'Synchronisiere…' : 'Aus GLPI synchronisieren'}
                    </Button>
                    <Button variant="primary" onClick={handleNewTicket}>
                        Neues Ticket
                    </Button>
                </div>
            </div>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}

            {tickets.length === 0 ? (
                <p className="text-muted">Noch keine Tickets vorhanden.</p>
            ) : (
                <HudPanel title="Ticketliste">
<Table hover responsive className="hud-table">
                    <thead>
                    <tr>
                        <th>Titel</th>
                        <th>Techniker</th>
                        <th>Szenario</th>
                        <th>Status</th>
                        <th>Erstellt am</th>

                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    {tickets.map((ticket) => (
                        <tr key={ticket.id}>
                            <td>{ticket.title}</td>
                            <td>{ticket.technician}</td>
                            <td>{SCENARIO_TYPE_LABELS[ticket.scenarioType]}</td>
                            <td>
                                <Badge bg={ticketStatusBadgeVariante(ticket.status)}>
                                    {TICKET_STATUS_LABELS[ticket.status]}
                                </Badge>
                            </td>
                            <td>{formatDate(ticket.createdAt)}</td>
                            <td>
                                <Button variant="outline-secondary" size="sm" onClick={() => handleEdit(ticket)}>
                                    Bearbeiten
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
                    <Modal.Title>{editedTicket ? 'Ticket bearbeiten' : 'Neues Ticket'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Titel</Form.Label>
                            <Form.Control
                                type="text"
                                value={form.title}

                                onChange={(e) => setForm({ ...form, title: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Beschreibung</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.description}
                                onChange={(e) => setForm({ ...form, description: e.target.value })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Techniker</Form.Label>
                            <Form.Control
                                type="text"
                                value={form.technician}
                                onChange={(e) => setForm({ ...form, technician: e.target.value })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Status</Form.Label>
                            <Form.Select
                                value={form.status}

                                onChange={(e) => setForm({ ...form, status: e.target.value as TicketStatus })}
                            >

                                {Object.entries(TICKET_STATUS_LABELS).map(([value, label]) => (
                                    <option key={value} value={value}>
                                        {label}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Szenario</Form.Label>
                            <Form.Select
                                value={form.scenarioType}
                                onChange={(e) => setForm({ ...form, scenarioType: e.target.value as ScenarioType })}
                            >
                                {Object.entries(SCENARIO_TYPE_LABELS).map(([value, label]) => (
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

                    <Button variant="primary" onClick={handleSave} disabled={saving || !form.title}>
                        {saving ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}
