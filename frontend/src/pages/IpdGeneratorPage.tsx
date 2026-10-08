import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Form, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import { formatDate, ipdStatusBadgeVariante } from '../utils/formatting';
import { IPD_DOCUMENT_STATUS_LABELS, type IpdDocumentDto, type TicketDto } from '../api/types';

export function IpdGeneratorPage() {
    const navigate = useNavigate();

    const [ipdDocuments, setDocuments] = useState<IpdDocumentDto[]>([]);
    const [tickets, setTickets] = useState<TicketDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const [ticketSelection, setTicketSelection] = useState('');
    const [creating, setCreating] = useState(false);

    async function loadData() {
        try {
            const [loadedDocuments, loadedTickets] = await Promise.all([
                api.get<IpdDocumentDto[]>('/api/ipd'),
                api.get<TicketDto[]>('/api/tickets'),
            ]);
            setDocuments(loadedDocuments);
            setTickets(loadedTickets);

            setTicketSelection((previous) => previous || loadedTickets[0]?.id || '');
        } catch (error) {
            setErrorMessage('Daten konnten nicht geladen werden.');
            console.error(error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        let aborted = false;
        Promise.all([api.get<IpdDocumentDto[]>('/api/ipd'), api.get<TicketDto[]>('/api/tickets')])
            .then(([loadedDocuments, loadedTickets]) => {
                if (aborted) return;
                setDocuments(loadedDocuments);
                setTickets(loadedTickets);

                setTicketSelection((previous) => previous || loadedTickets[0]?.id || '');
            })
            .catch((error) => {
                if (aborted) return;
                setErrorMessage('Daten konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setLoading(false);
            });
        return () => {
            aborted = true;
        };
    }, []);

    function ticketTitle(ticketId: string): string {
        return tickets.find((t) => t.id === ticketId)?.title ?? '(unbekanntes Ticket)';
    }

    function findExistingDraft(ticketId: string): IpdDocumentDto | undefined {
        return ipdDocuments.find((ipdDocument) => ipdDocument.ticketId === ticketId && ipdDocument.status === 'DRAFT');
    }

    async function handleDraftCreate() {
        if (!ticketSelection) {
            return;
        }
        const existingDraft = findExistingDraft(ticketSelection);
        if (
            existingDraft &&
            !window.confirm(
                `Für "${ticketTitle(ticketSelection)}" existiert bereits ein Entwurf ("${existingDraft.title}"). Trotzdem einen weiteren erzeugen?`,
            )
        ) {
            return;
        }

        setCreating(true);
        setErrorMessage(null);
        try {
            const newDocument = await api.post<IpdDocumentDto>(`/api/ipd/from-ticket/${ticketSelection}`);

            void navigate(`/ipd/${newDocument.id}`);
        } catch (error) {
            setErrorMessage('Entwurf konnte nicht erzeugt werden.');
            console.error(error);
        } finally {
            setCreating(false);
        }
    }

    async function handleDelete(ipdDocument: IpdDocumentDto) {
        if (!window.confirm(`IPD-Dokument "${ipdDocument.title}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/ipd/${ipdDocument.id}`);
            await loadData();
        } catch (error) {
            setErrorMessage('IPD-Dokument konnte nicht gelöscht werden.');
            console.error(error);
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
            <h1 className="mb-4">IPD-Generator</h1>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}

            {tickets.length === 0 ? (
                <Alert variant="info">
                    Es existiert noch kein Ticket - lege zuerst ein Ticket an, bevor du einen IPD-Entwurf
                    erzeugen kannst.
                </Alert>
            ) : (
                <HudPanel title="Neuen Entwurf erzeugen" className="mb-4">
<div className="d-flex flex-wrap align-items-end gap-3">
                    <Form.Group style={{ maxWidth: 320 }}>
                        <Form.Label>Ticket</Form.Label>
                        <Form.Select value={ticketSelection} onChange={(e) => setTicketSelection(e.target.value)}>
                            {tickets.map((ticket) => (
                                <option key={ticket.id} value={ticket.id}>
                                    {ticket.title}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>
                    <Button variant="primary" onClick={handleDraftCreate} disabled={creating}>
                        {creating ? 'Erzeuge…' : 'Entwurf aus Ticket erzeugen'}
                    </Button>
                </div>
                </HudPanel>
            )}

            {ipdDocuments.length === 0 ? (
                <Alert variant="dark" className="text-center">
                    Noch keine IPD-Dokumente vorhanden.
                </Alert>
            ) : (
                <HudPanel title="Vorhandene IPD-Dokumente">
<Table hover responsive className="hud-table">
                    <thead>
                    <tr>
                        <th>Titel</th>
                        <th>Ticket</th>
                        <th>Status</th>
                        <th>Erstellt am</th>
                        <th>Aktualisiert am</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    {ipdDocuments.map((ipdDocument) => (
                        <tr key={ipdDocument.id}>
                            <td>{ipdDocument.title}</td>
                            <td>{ticketTitle(ipdDocument.ticketId)}</td>
                            <td>
                                <Badge bg={ipdStatusBadgeVariante(ipdDocument.status)}>
                                    {IPD_DOCUMENT_STATUS_LABELS[ipdDocument.status]}
                                </Badge>
                            </td>
                            <td>{formatDate(ipdDocument.createdAt)}</td>
                            <td>{formatDate(ipdDocument.updatedAt)}</td>
                            <td>

                                <div className="d-flex gap-2">
                                    <Button variant="outline-secondary" size="sm" onClick={() => navigate(`/ipd/${ipdDocument.id}`)}>
                                        Öffnen
                                    </Button>
                                    <Button variant="outline-danger" size="sm" onClick={() => handleDelete(ipdDocument)}>
                                        Löschen
                                    </Button>
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </Table>
</HudPanel>
            )}
        </div>
    );
}
