import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, Badge, Button, Form, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import { formatDate, ipdStatusBadgeVariant } from '../utils/formatting';
import { type IpdDocumentDto, type TicketDto } from '../api/types';

export function IpdGeneratorPage() {
    const { t } = useTranslation('ipd');
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
            setErrorMessage(t('generator.loadError'));
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
                setErrorMessage(t('generator.loadError'));
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setLoading(false);
            });
        return () => {
            aborted = true;
        };
    }, [t]);

    function ticketTitle(ticketId: string): string {
        return tickets.find((ticket) => ticket.id === ticketId)?.title ?? t('generator.unknownTicket');
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
                t('generator.duplicateDraftConfirm', { ticket: ticketTitle(ticketSelection), draft: existingDraft.title }),
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
            setErrorMessage(t('generator.createError'));
            console.error(error);
        } finally {
            setCreating(false);
        }
    }

    async function handleDelete(ipdDocument: IpdDocumentDto) {
        if (!window.confirm(t('generator.deleteConfirm', { title: ipdDocument.title }))) {
            return;
        }
        try {
            await api.delete(`/api/ipd/${ipdDocument.id}`);
            await loadData();
        } catch (error) {
            setErrorMessage(t('generator.deleteError'));
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
            <h1 className="mb-4">{t('generator.title')}</h1>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}

            {tickets.length === 0 ? (
                <Alert variant="info">
                    {t('generator.noTickets')}
                </Alert>
            ) : (
                <HudPanel title={t('generator.newDraft')} className="mb-4">
<div className="d-flex flex-wrap align-items-end gap-3">
                    <Form.Group style={{ maxWidth: 320 }}>
                        <Form.Label>{t('generator.ticket')}</Form.Label>
                        <Form.Select value={ticketSelection} onChange={(e) => setTicketSelection(e.target.value)}>
                            {tickets.map((ticket) => (
                                <option key={ticket.id} value={ticket.id}>
                                    {ticket.title}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>
                    <Button variant="primary" onClick={handleDraftCreate} disabled={creating}>
                        {creating ? t('generator.creating') : t('generator.createFromTicket')}
                    </Button>
                </div>
                </HudPanel>
            )}

            {ipdDocuments.length === 0 ? (
                <Alert variant="dark" className="text-center">
                    {t('generator.empty')}
                </Alert>
            ) : (
                <HudPanel title={t('generator.existing')}>
<Table hover responsive className="hud-table">
                    <thead>
                    <tr>
                        <th>{t('generator.columns.title')}</th>
                        <th>{t('generator.columns.ticket')}</th>
                        <th>{t('generator.columns.status')}</th>
                        <th>{t('generator.columns.createdAt')}</th>
                        <th>{t('generator.columns.updatedAt')}</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    {ipdDocuments.map((ipdDocument) => (
                        <tr key={ipdDocument.id}>
                            <td>{ipdDocument.title}</td>
                            <td>{ticketTitle(ipdDocument.ticketId)}</td>
                            <td>
                                <Badge bg={ipdStatusBadgeVariant(ipdDocument.status)}>
                                    {t(`status.ipd.${ipdDocument.status}`, { ns: 'common' })}
                                </Badge>
                            </td>
                            <td>{formatDate(ipdDocument.createdAt)}</td>
                            <td>{formatDate(ipdDocument.updatedAt)}</td>
                            <td>

                                <div className="d-flex gap-2">
                                    <Button variant="outline-secondary" size="sm" onClick={() => navigate(`/ipd/${ipdDocument.id}`)}>
                                        {t('generator.open')}
                                    </Button>
                                    <Button variant="outline-danger" size="sm" onClick={() => handleDelete(ipdDocument)}>
                                        {t('generator.delete')}
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
