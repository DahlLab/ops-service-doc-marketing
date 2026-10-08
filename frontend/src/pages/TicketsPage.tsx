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

// Ausgangszustand des Formulars beim Anlegen eines NEUEN Tickets -
// als eigene Konstante statt inline, damit ich sie sowohl beim
// Öffnen des "Neu"-Modals als auch als Vergleichswert wiederverwenden
// kann, ohne das Objekt zweimal von Hand hinzuschreiben.
const EMPTY_FORM: TicketFormData = {
    title: '',
    description: '',
    status: 'NEW',
    technician: '',
    scenarioType: 'SERVER_MAINTENANCE',
};

// Seite für den Bereich "Tickets" (entspricht meinem TicketController
// im Backend). Zeigt alle Tickets als Tabelle, bietet einen Button zum
// manuellen GLPI-Sync, sowie Anlegen/Bearbeiten über ein gemeinsames
// Modal. KEIN Löschen, weil mein Backend für Tickets bewusst keinen
// DELETE-Endpoint hat (ein einmal importiertes/angelegtes Ticket soll
// nicht einfach verschwinden können, nur der Status kann sich ändern).
export function TicketsPage() {
    // Die aktuelle Ticket-Liste, so wie sie vom Backend kommt.
    const [tickets, setTickets] = useState<TicketDto[]>([]);

    // true, solange die allererste GET-Anfrage beim Laden der Seite
    // läuft - steuert, ob ich den großen zentrierten Spinner oder die
    // eigentliche Seite anzeige.
    const [loading, setLoading] = useState(true);

    // Eigene Ladezustände für Sync und Speichern (statt alles über
    // "loading" zu steuern), damit z.B. der Sync-Button einen eigenen
    // "Synchronisiere…"-Text zeigen kann, während der Rest der Seite
    // ganz normal nutzbar bleibt.
    const [syncing, setSyncing] = useState(false);
    const [saving, setSaving] = useState(false);

    // Fehlertext für die rote Alert-Box oben auf der Seite. null
    // bedeutet "kein Fehler" - ich nutze eine eigene Variable statt z.B.
    // try/catch direkt im JSX, weil ich den Fehler über mehrere
    // Funktionen hinweg (laden, syncen, speichern) konsistent anzeigen will.
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Steuert Sichtbarkeit des Anlegen/Bearbeiten-Modals.
    const [modalOpen, setModalOpen] = useState(false);
    // null = ich lege gerade ein NEUES Ticket an; ist hier ein Ticket
    // gesetzt, bin ich im Bearbeiten-Modus für genau dieses Ticket (siehe
    // handleSpeichern, wo ich danach entscheide, ob POST oder PUT nötig ist).
    const [editedTicket, setEditedTicket] = useState<TicketDto | null>(null);
    // Die aktuellen Formularwerte im Modal, unabhängig davon, ob ich
    // gerade anlege oder bearbeite.
    const [form, setForm] = useState<TicketFormData>(EMPTY_FORM);

    // Lädt die komplette Ticket-Liste neu vom Backend. Als eigene,
    // benannte Funktion (statt direkt im useEffect), weil ich sie an
    // DREI Stellen brauche: beim ersten Laden der Seite, nach einem
    // erfolgreichen GLPI-Sync, und nach dem Speichern eines Tickets -
    // so muss ich den fetch-Aufruf nicht dreimal duplizieren (DRY).
    async function loadTickets() {
        try {
            const loadedTickets = await api.get<TicketDto[]>('/api/tickets');
            setTickets(loadedTickets);
            // Einen eventuell vorherigen Fehler wieder zurücksetzen, sobald
            // ein Ladevorgang erfolgreich war.
            setErrorMessage(null);
        } catch (error) {
            setErrorMessage('Tickets konnten nicht geladen werden.');
            // console.error zusätzlich zur UI-Fehlermeldung, damit ich beim
            // Debuggen in der Browser-Konsole den vollen Fehler/Stacktrace sehe.
            console.error(error);
        } finally {
            setLoading(false);
        }
    }

    // Leeres Dependency-Array ([]): dieser Effect soll nur EINMAL laufen,
    // wenn die Komponente zum ersten Mal gerendert wird - nicht bei jedem
    // Re-Render (z.B. wenn sich "tickets" selbst ändert, das würde sonst
    // eine Endlosschleife auslösen).
    // Die Anfrage steht hier direkt im Effect (statt ladeTickets() aufzurufen):
    // React will, dass State im Effect nur in einem Callback gesetzt wird, wenn
    // die Daten ankommen - nicht synchron beim Start des Effects. `abgebrochen`
    // verhindert, dass ich State setze, wenn die Seite schon verlassen wurde.
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

    // Wird vom "Aus GLPI synchronisieren"-Button aufgerufen. Ruft den
    // Sync-Endpoint auf (der laut TicketController aktuell neue Tickets
    // anlegt, aber bestehende noch nicht aktualisiert) und lädt danach
    // die Liste neu, damit die frisch importierten Tickets sofort sichtbar sind.
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

    // Öffnet das Modal im "Neu anlegen"-Modus: bearbeitetesTicket wird
    // auf null gesetzt (wichtig für handleSpeichern!) und das Formular
    // auf die leeren Ausgangswerte zurückgesetzt - sonst würden beim
    // zweiten Öffnen noch die Werte vom letzten bearbeiteten Ticket drinstehen.
    function handleNewTicket() {
        setEditedTicket(null);
        setForm(EMPTY_FORM);
        setModalOpen(true);
    }

    // Öffnet das Modal im "Bearbeiten"-Modus, vorbefüllt mit den
    // aktuellen Werten des angeklickten Tickets. Ich baue das Formular-
    // Objekt hier bewusst neu zusammen (statt das ganze TicketDto
    // reinzureichen), weil TicketFormData id und erstelltAm nicht kennt -
    // so kann ich nicht versehentlich eine veraltete erstelltAm ans
    // Backend zurückschicken.
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

    // Speichert das Formular - je nachdem, ob bearbeitetesTicket gesetzt
    // ist, entweder per PUT (Bearbeiten, ID aus dem bestehenden Ticket)
    // oder per POST (Neu anlegen, Backend vergibt die ID selbst).
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
            // Liste neu laden statt das neue/geänderte Ticket manuell in den
            // State einzufügen - etwas mehr Netzwerk-Traffic, aber dafür bin
            // ich sicher, dass die Tabelle immer exakt dem Datenbankstand
            // entspricht (z.B. falls das Backend noch Felder ergänzt/normalisiert).
            await loadTickets();
        } catch (error) {
            // 400 Bad Request kommt typischerweise von der @Valid-Prüfung im
            // Backend (z.B. wenn titel fehlt) - dafür zeige ich eine
            // konkretere Fehlermeldung als für andere Fehler.
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

    // Solange die erste Ladeanfrage noch läuft, zeige ich nur den
    // Spinner statt einer leeren Tabelle - vermeidet ein kurzes
    // "Flackern" von "keine Tickets vorhanden" direkt gefolgt von der
    // echten Liste.
    if (loading) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        );
    }

    return (
        <div className="py-4">
            {/* Überschrift + Action-Buttons in einer Zeile, rechts
            ausgerichtet - üblichs Muster für Listen-Seiten. */}
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

            {/* Fehler-Box wird nur gerendert, wenn tatsächlich ein Fehler
            vorliegt (fehler ist dann nicht null). */}
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
                        {/* Leere Kopfzelle für die Aktions-Spalte - braucht
                    keine Überschrift, der "Bearbeiten"-Button spricht
                    für sich. */}
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    {tickets.map((ticket) => (
                        // key={ticket.id}: React braucht einen stabilen,
                        // eindeutigen Key pro Listenelement, um beim Neu-
                        // Rendern effizient zu erkennen, welche Zeilen sich
                        // geändert haben - die MongoDB-ID eignet sich dafür perfekt.
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

            {/* EIN Modal für Anlegen UND Bearbeiten statt zwei getrennter
            Formulare - der einzige Unterschied zwischen beiden Fällen
            ist, ob bearbeitetesTicket gesetzt ist (siehe
            handleSpeichern oben), das Formular-Markup ist identisch. */}
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
                                // Ich baue bei jeder Eingabe ein neues Formular-
                                // Objekt per Spread (...formular), statt das
                                // bestehende Objekt zu mutieren - React erkennt
                                // Zustandsänderungen nur zuverlässig bei neuen
                                // Objektreferenzen, nicht bei mutierten bestehenden.
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
                                // e.target.value ist vom Typ string - ich caste
                                // explizit auf TicketStatus, weil ich über die
                                // <option>-Werte unten selbst sicherstelle, dass
                                // dort nur gültige TicketStatus-Werte stehen können.
                                onChange={(e) => setForm({ ...form, status: e.target.value as TicketStatus })}
                            >
                                {/* Ich generiere die Optionen aus TICKET_STATUS_LABELS
                      statt sie einzeln hinzuschreiben - so bleibt die
                      Liste automatisch synchron mit dem Typ TicketStatus
                      in types.ts, ohne dass ich zwei Stellen pflegen muss. */}
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
                    {/* Button deaktiviert, solange gespeichert wird (verhindert
                Doppelklick-Doppelanlage) oder solange der Titel leer
                ist (Pflichtfeld laut Backend-Validierung @NotBlank). */}
                    <Button variant="primary" onClick={handleSave} disabled={saving || !form.title}>
                        {saving ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}