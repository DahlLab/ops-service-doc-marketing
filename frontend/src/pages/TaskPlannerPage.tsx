import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Spinner, Table } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import { formatiereDatum, formatiereZieldatum, taskStatusBadgeVariante } from '../utils/formatierung';
import {
    TASK_STATUS_LABELS,
    type TaskDto,
    type TaskFormData,
    type TaskStatus,
    type TicketDto,
} from '../api/types';

const LEERES_FORMULAR: TaskFormData = {
    ticketId: '',
    thema: '',
    naechsteSchritte: '',
    zieldatum: null,
    status: 'OFFEN',
};

// Seite für den Bereich "TaskPlanner" (entspricht meinem
// TaskController im Backend). Anders als bei Tickets gibt es hier
// einen echten DELETE-Endpoint, also biete ich Löschen mit an. Zudem
// kann ich die Liste nach einem bestimmten Ticket filtern, genau wie
// es der GET /api/tasks?ticketId=...-Endpoint unterstützt.
export function TaskPlannerPage() {
    const [tasks, setTasks] = useState<TaskDto[]>([]);
    // Ich lade zusätzlich ALLE Tickets mit, aus zwei Gründen:
    // 1. In der Tabelle will ich den Ticket-TITEL anzeigen statt der
    //    rohen ticketId (die wäre für den Nutzer nicht lesbar).
    // 2. Im Formular brauche ich ein Auswahlfeld, aus dem der Nutzer ein
    //    bestehendes Ticket auswählt, statt die ID von Hand einzutippen.
    const [tickets, setTickets] = useState<TicketDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [fehler, setFehler] = useState<string | null>(null);

    // Aktuell gewählter Filter: leerer String bedeutet "alle Tickets",
    // sonst die ID des Tickets, nach dem gefiltert wird.
    const [ticketFilter, setTicketFilter] = useState('');

    const [modalOffen, setModalOffen] = useState(false);
    // null = Neuanlage, sonst der Task, der gerade bearbeitet wird
    // (siehe handleSpeichern für die POST/PUT-Entscheidung).
    const [bearbeiteterTask, setBearbeiteterTask] = useState<TaskDto | null>(null);
    const [formular, setFormular] = useState<TaskFormData>(LEERES_FORMULAR);

    // Lädt die Task-Liste, optional gefiltert nach ticketId. Als eigene
    // Funktion mit Parameter (statt den State direkt zu lesen), weil ich
    // sie sowohl beim ersten Laden (ticketId = '') als auch beim
    // Filterwechsel und nach dem Speichern/Löschen mit dem jeweils
    // AKTUELLEN Filterwert aufrufen will, ohne auf einen State-Update
    // warten zu müssen, der asynchron ist.
    async function ladeTasks(ticketId: string) {
        try {
            const pfad = ticketId ? `/api/tasks?ticketId=${ticketId}` : '/api/tasks';
            const geladeneTasks = await api.get<TaskDto[]>(pfad);
            setTasks(geladeneTasks);
            setFehler(null);
        } catch (error) {
            setFehler('Tasks konnten nicht geladen werden.');
            console.error(error);
        } finally {
            setLoading(false);
        }
    }

    // Beim ersten Rendern der Seite lade ich einmalig die Ticket-Liste
    // (fürs Auswahlfeld/die Anzeige) UND die ungefilterte Task-Liste.
    // Die Anfragen stehen direkt im Effect: State wird nur im Callback gesetzt,
    // wenn die Daten ankommen (nicht synchron im Effect-Start), und `abgebrochen`
    // schützt davor, State nach dem Verlassen der Seite zu setzen.
    useEffect(() => {
        let abgebrochen = false;
        api.get<TicketDto[]>('/api/tickets').then(setTickets).catch(console.error);
        api.get<TaskDto[]>('/api/tasks')
            .then((geladeneTasks) => {
                if (abgebrochen) return;
                setTasks(geladeneTasks);
                setFehler(null);
            })
            .catch((error) => {
                if (abgebrochen) return;
                setFehler('Tasks konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!abgebrochen) setLoading(false);
            });
        return () => {
            abgebrochen = true;
        };
    }, []);

    // Wird aufgerufen, wenn der Nutzer im Filter-Dropdown ein anderes
    // Ticket (oder "Alle Tickets") auswählt. Ich setze den Filter-State
    // UND lade sofort die passende Liste nach - bewusst OHNE einen
    // separaten "Filtern"-Button, das Dropdown allein reicht für diese
    // einfache Filterung aus.
    function handleFilterChange(ticketId: string) {
        setTicketFilter(ticketId);
        setLoading(true);
        void ladeTasks(ticketId);
    }

    // Sucht zu einer ticketId den passenden Ticket-Titel für die
    // Tabellen-Anzeige. Der Fallback-Text greift nur in dem
    // (theoretisch möglichen) Fall, dass ein Ticket zwischenzeitlich
    // gelöscht wurde, der Task aber noch existiert.
    function ticketTitel(ticketId: string): string {
        return tickets.find((t) => t.id === ticketId)?.titel ?? '(unbekanntes Ticket)';
    }

    // Öffnet das Modal im "Neu anlegen"-Modus. Ist gerade ein
    // Ticket-Filter aktiv, übernehme ich dessen Ticket direkt als
    // Vorauswahl ins Formular - spart einen Klick, wenn ich mehrere
    // Tasks nacheinander zum selben Ticket erfassen will. Ist kein
    // Filter aktiv, nehme ich ersatzweise das erste Ticket der Liste,
    // damit das Auswahlfeld nie leer/ungültig ist.
    //
    // "ticketIdVorauswahl" kann ich optional explizit übergeben - das
    // nutze ich für den Button in der Leer-Zeile der Tabelle, der
    // IMMER zum gerade gefilterten Ticket gehört, auch falls ich diese
    // Funktion später mal von woanders ohne aktiven Filter aufrufe.
    function handleNeuerTask(ticketIdVorauswahl?: string) {
        setBearbeiteterTask(null);
        setFormular({
            ...LEERES_FORMULAR,
            ticketId: ticketIdVorauswahl || ticketFilter || tickets[0]?.id || '',
        });
        setModalOffen(true);
    }

    // Öffnet das Modal im Bearbeiten-Modus, vorbefüllt mit den
    // aktuellen Werten - erfasstAm/erledigtAm lasse ich bewusst aus
    // (siehe TaskFormData in types.ts), die pflegt mein Backend selbst.
    function handleBearbeiten(task: TaskDto) {
        setBearbeiteterTask(task);
        setFormular({
            ticketId: task.ticketId,
            thema: task.thema,
            naechsteSchritte: task.naechsteSchritte,
            zieldatum: task.zieldatum,
            status: task.status,
        });
        setModalOffen(true);
    }

    async function handleSpeichern() {
        setSaving(true);
        setFehler(null);
        try {
            if (bearbeiteterTask) {
                await api.put<TaskDto>(`/api/tasks/${bearbeiteterTask.id}`, formular);
            } else {
                await api.post<TaskDto>('/api/tasks', formular);
            }
            setModalOffen(false);
            // Mit dem AKTUELLEN Filter neu laden, damit ich nach dem
            // Speichern wieder genau die Liste sehe, die zur aktuellen
            // Filterauswahl passt.
            await ladeTasks(ticketFilter);
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setFehler('Bitte Ticket und Thema ausfüllen.');
            } else {
                setFehler('Task konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    // Löscht einen Task nach Bestätigung. window.confirm() statt eines
    // eigenen Bestätigungs-Modals - für eine einzelne, schnell zu
    // treffende Entscheidung wie "löschen ja/nein" reicht der
    // eingebaute Browser-Dialog aus, ohne dass ich dafür extra
    // Modal-State verwalten muss.
    async function handleLoeschen(task: TaskDto) {
        if (!window.confirm(`Task "${task.thema}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/tasks/${task.id}`);
            await ladeTasks(ticketFilter);
        } catch (error) {
            setFehler('Task konnte nicht gelöscht werden.');
            console.error(error);
        }
    }

    // Ich prüfe zusätzlich "tasks.length === 0", damit beim bloßen
    // Filterwechsel (loading wird kurz true) nicht nochmal der komplette
    // große Spinner über der ganzen Seite erscheint, sondern nur beim
    // allerersten Laden.
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
                {/* Solange es gar kein Ticket gibt, kann ich keinen Task
              anlegen (ein Task braucht zwingend eine ticketId) - der
              Button ist dann deaktiviert statt einen Fehler zu
              provozieren. */}
                <Button variant="primary" onClick={() => handleNeuerTask()} disabled={tickets.length === 0}>
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
                            {ticket.titel}
                        </option>
                    ))}
                </Form.Select>
            </Form.Group>

            {fehler && <Alert variant="danger">{fehler}</Alert>}

            {/* Die Tabelle mit Kopfzeile zeige ich jetzt IMMER an, auch
            wenn tasks leer ist - so bleibt die Spaltenübersicht
            (Thema, Ticket, Nächste Schritte, ...) sichtbar, egal ob
            gerade "Alle Tickets" oder ein einzelnes Ticket ohne Tasks
            ausgewählt ist. Nur der Tabellenkörper unterscheidet sich:
            entweder die echten Zeilen, oder eine einzelne Hinweiszeile
            über alle Spalten hinweg (colSpan={8}) MIT einem Button, über
            den ich direkt aus der leeren Ansicht heraus einen Task
            anlegen kann - vorher gab es dafür nur den Button oben am
            Seitenkopf, dessen Bezug zum gerade gefilterten Ticket nicht
            offensichtlich war. */}
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
                                    ? `Für "${ticketTitel(ticketFilter)}" sind noch keine Tasks erfasst.`
                                    : 'Keine Tasks vorhanden.'}
                            </div>
                            {/* Ohne aktiven Filter übergebe ich keine ticketId -
                            handleNeuerTask() greift dann automatisch auf das
                            erste Ticket der Liste zurück (siehe dort). Mit
                            aktivem Filter übergebe ich ihn explizit, damit der
                            Button unzweifelhaft zu GENAU diesem Ticket gehört,
                            auch wenn sich ticketFilter zwischen Klick und
                            Ausführung ändern sollte. */}
                            <Button
                                variant="outline-primary"
                                size="sm"
                                onClick={() => handleNeuerTask(ticketFilter)}
                                disabled={tickets.length === 0}
                            >
                                + Task {ticketFilter ? 'für dieses Ticket' : ''} anlegen
                            </Button>
                        </td>
                    </tr>
                ) : (
                    tasks.map((task) => (
                        <tr key={task.id}>
                            <td>{task.thema}</td>
                            <td>{ticketTitel(task.ticketId)}</td>
                            <td>{task.naechsteSchritte}</td>
                            <td>{formatiereZieldatum(task.zieldatum)}</td>
                            <td>
                                <Badge bg={taskStatusBadgeVariante(task.status)}>{TASK_STATUS_LABELS[task.status]}</Badge>
                            </td>
                            <td>{formatiereDatum(task.erfasstAm)}</td>
                            <td>{formatiereDatum(task.erledigtAm)}</td>
                            <td>
                                {/* Flex sitzt im div, damit die td eine echte Tabellenzelle bleibt und die Linie durchgeht */}
                                <div className="d-flex gap-2">
                                    <Button variant="outline-secondary" size="sm" onClick={() => handleBearbeiten(task)}>
                                        Bearbeiten
                                    </Button>
                                    <Button variant="outline-danger" size="sm" onClick={() => handleLoeschen(task)}>
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

            <Modal show={modalOffen} onHide={() => setModalOffen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{bearbeiteterTask ? 'Task bearbeiten' : 'Neuer Task'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Ticket</Form.Label>
                            <Form.Select
                                value={formular.ticketId}
                                onChange={(e) => setFormular({ ...formular, ticketId: e.target.value })}
                            >
                                {tickets.map((ticket) => (
                                    <option key={ticket.id} value={ticket.id}>
                                        {ticket.titel}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Thema</Form.Label>
                            <Form.Control
                                type="text"
                                value={formular.thema}
                                onChange={(e) => setFormular({ ...formular, thema: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Nächste Schritte</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.naechsteSchritte}
                                onChange={(e) => setFormular({ ...formular, naechsteSchritte: e.target.value })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Zieldatum</Form.Label>
                            <Form.Control
                                type="date"
                                // "?? ''" statt null: ein <input type="date"> kann
                                // mit null als value nicht umgehen (React würde
                                // eine Warnung werfen), ein leerer String zeigt das
                                // Feld dagegen einfach leer an.
                                value={formular.zieldatum ?? ''}
                                // Tippt der Nutzer das Datum wieder komplett raus,
                                // kommt hier ein leerer String an - den wandle ich
                                // zurück in null, damit zieldatum entweder ein
                                // gültiges Datum oder wirklich "nicht gesetzt" ist.
                                onChange={(e) => setFormular({ ...formular, zieldatum: e.target.value || null })}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Status</Form.Label>
                            <Form.Select
                                value={formular.status}
                                onChange={(e) => setFormular({ ...formular, status: e.target.value as TaskStatus })}
                            >
                                {Object.entries(TASK_STATUS_LABELS).map(([wert, label]) => (
                                    <option key={wert} value={wert}>
                                        {label}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setModalOffen(false)}>
                        Abbrechen
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSpeichern}
                        disabled={saving || !formular.thema || !formular.ticketId}
                    >
                        {saving ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}