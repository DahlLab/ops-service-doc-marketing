import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Form, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import { formatiereDatum, ipdStatusBadgeVariante } from '../utils/formatierung';
import { IPD_DOCUMENT_STATUS_LABELS, type IpdDocumentDto, type TicketDto } from '../api/types';

// Seite für den Bereich "IPD-Generator" (entspricht IpdDocumentController
// im Backend) - der "Generieren"-Teil aus meiner geplanten Seitenreihenfolge
// Home -> Tickets -> TaskPlanner -> IPD-Generator (Generieren) -> Ergebnis.
// Diese Seite zeigt alle bereits erzeugten IPD-Dokumente und bietet den
// Button, aus einem Ticket einen neuen Entwurf zu erzeugen. Das eigentliche
// Ausfüllen der Abschnitte und der PDF-Export passieren dann auf der
// Detailseite IpdDocumentPage ("Ergebnis"), zu der ich nach dem Erzeugen
// automatisch weiterspringe.
export function IpdGeneratorPage() {
    const navigate = useNavigate();

    const [dokumente, setDokumente] = useState<IpdDocumentDto[]>([]);
    const [tickets, setTickets] = useState<TicketDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [fehler, setFehler] = useState<string | null>(null);

    // Filter für die Tabelle (wie bei Task/Checklist) UND gleichzeitig
    // die Auswahl, aus welchem Ticket ich einen neuen Entwurf erzeugen
    // will - beides zusammenzulegen spart ein zweites Dropdown, da ich
    // beim Erzeugen eines Entwurfs ohnehin meistens genau das Ticket im
    // Blick habe, nach dem ich gerade filtere.
    const [ticketAuswahl, setTicketAuswahl] = useState('');
    const [erzeugeLaeuft, setErzeugeLaeuft] = useState(false);

    async function ladeDaten() {
        try {
            const [geladeneDokumente, geladeneTickets] = await Promise.all([
                api.get<IpdDocumentDto[]>('/api/ipd'),
                api.get<TicketDto[]>('/api/tickets'),
            ]);
            setDokumente(geladeneDokumente);
            setTickets(geladeneTickets);
            // Erstbefüllung der Auswahl mit dem ersten Ticket, damit der
            // "Entwurf erzeugen"-Button nicht erst nach manueller Auswahl
            // nutzbar wird.
            setTicketAuswahl((bisher) => bisher || geladeneTickets[0]?.id || '');
        } catch (error) {
            setFehler('Daten konnten nicht geladen werden.');
            console.error(error);
        } finally {
            setLoading(false);
        }
    }

    // Erstes Laden: Anfrage direkt im Effect, State wird nur im Callback gesetzt, wenn
    // die Daten ankommen. `abgebrochen` schützt davor, State nach dem Verlassen der
    // Seite zu setzen.
    useEffect(() => {
        let abgebrochen = false;
        Promise.all([api.get<IpdDocumentDto[]>('/api/ipd'), api.get<TicketDto[]>('/api/tickets')])
            .then(([geladeneDokumente, geladeneTickets]) => {
                if (abgebrochen) return;
                setDokumente(geladeneDokumente);
                setTickets(geladeneTickets);
                // Erstbefüllung der Auswahl mit dem ersten Ticket
                setTicketAuswahl((bisher) => bisher || geladeneTickets[0]?.id || '');
            })
            .catch((error) => {
                if (abgebrochen) return;
                setFehler('Daten konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!abgebrochen) setLoading(false);
            });
        return () => {
            abgebrochen = true;
        };
    }, []);

    function ticketTitel(ticketId: string): string {
        return tickets.find((t) => t.id === ticketId)?.titel ?? '(unbekanntes Ticket)';
    }

    // Prüft, ob für das gerade ausgewählte Ticket bereits ein Entwurf
    // (Status ENTWURF) existiert - rein informativ als Warnhinweis, das
    // Backend erlaubt mehrere IPD-Dokumente pro Ticket durchaus (z.B.
    // wenn eine Wartung sich wiederholt), ich will nur nicht aus
    // Versehen einen zweiten Entwurf für dieselbe laufende Wartung
    // anlegen.
    function bestehenderEntwurf(ticketId: string): IpdDocumentDto | undefined {
        return dokumente.find((dokument) => dokument.ticketId === ticketId && dokument.status === 'ENTWURF');
    }

    async function handleEntwurfErzeugen() {
        if (!ticketAuswahl) {
            return;
        }
        const vorhandenerEntwurf = bestehenderEntwurf(ticketAuswahl);
        if (
            vorhandenerEntwurf &&
            !window.confirm(
                `Für "${ticketTitel(ticketAuswahl)}" existiert bereits ein Entwurf ("${vorhandenerEntwurf.titel}"). Trotzdem einen weiteren erzeugen?`,
            )
        ) {
            return;
        }

        setErzeugeLaeuft(true);
        setFehler(null);
        try {
            const neuesDokument = await api.post<IpdDocumentDto>(`/api/ipd/from-ticket/${ticketAuswahl}`);
            // Direkt zur Detailseite springen, damit ich sofort mit dem
            // Ausfüllen der restlichen Abschnitte weitermachen kann,
            // statt erst wieder in der Liste danach suchen zu müssen.
            void navigate(`/ipd/${neuesDokument.id}`);
        } catch (error) {
            setFehler('Entwurf konnte nicht erzeugt werden.');
            console.error(error);
        } finally {
            setErzeugeLaeuft(false);
        }
    }

    async function handleLoeschen(dokument: IpdDocumentDto) {
        if (!window.confirm(`IPD-Dokument "${dokument.titel}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/ipd/${dokument.id}`);
            await ladeDaten();
        } catch (error) {
            setFehler('IPD-Dokument konnte nicht gelöscht werden.');
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

            {fehler && <Alert variant="danger">{fehler}</Alert>}

            {tickets.length === 0 ? (
                <Alert variant="info">
                    Es existiert noch kein Ticket - lege zuerst ein Ticket an, bevor du einen IPD-Entwurf
                    erzeugen kannst.
                </Alert>
            ) : (
                // Erzeugen-Bereich bewusst als eigene, auffällige Box oben
                // auf der Seite - das ist die Haupthandlung dieser Seite,
                // das Scrollen durch alte Dokumente kommt erst danach.
                <HudPanel title="Neuen Entwurf erzeugen" className="mb-4">
<div className="d-flex flex-wrap align-items-end gap-3">
                    <Form.Group style={{ maxWidth: 320 }}>
                        <Form.Label>Ticket</Form.Label>
                        <Form.Select value={ticketAuswahl} onChange={(e) => setTicketAuswahl(e.target.value)}>
                            {tickets.map((ticket) => (
                                <option key={ticket.id} value={ticket.id}>
                                    {ticket.titel}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>
                    <Button variant="primary" onClick={handleEntwurfErzeugen} disabled={erzeugeLaeuft}>
                        {erzeugeLaeuft ? 'Erzeuge…' : 'Entwurf aus Ticket erzeugen'}
                    </Button>
                </div>
                </HudPanel>
            )}

            {dokumente.length === 0 ? (
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
                    {dokumente.map((dokument) => (
                        <tr key={dokument.id}>
                            <td>{dokument.titel}</td>
                            <td>{ticketTitel(dokument.ticketId)}</td>
                            <td>
                                <Badge bg={ipdStatusBadgeVariante(dokument.status)}>
                                    {IPD_DOCUMENT_STATUS_LABELS[dokument.status]}
                                </Badge>
                            </td>
                            <td>{formatiereDatum(dokument.erstelltAm)}</td>
                            <td>{formatiereDatum(dokument.aktualisiertAm)}</td>
                            <td>
                                {/* Flex sitzt im div, damit die td eine echte Tabellenzelle bleibt und die Linie durchgeht */}
                                <div className="d-flex gap-2">
                                    <Button variant="outline-secondary" size="sm" onClick={() => navigate(`/ipd/${dokument.id}`)}>
                                        Öffnen
                                    </Button>
                                    <Button variant="outline-danger" size="sm" onClick={() => handleLoeschen(dokument)}>
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