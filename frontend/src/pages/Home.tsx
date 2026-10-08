import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Badge, Spinner } from 'react-bootstrap'
import {
    FaClipboardCheck,
    FaExternalLinkAlt,
    FaFileAlt,
    FaGithub,
    FaSyncAlt,
    FaTasks,
} from 'react-icons/fa'
import { useAuth } from '../auth/useAuth'
import { useGlpiUrl } from '../hooks/useGlpiUrl'
import { api } from '../api/api'
import type { ChecklistDto, IpdDocumentDto, TaskDto, TicketDto } from '../api/types'
import {
    IPD_DOCUMENT_STATUS_LABELS,
    TASK_STATUS_LABELS,
} from '../api/types'
import { formatDate, formatDueDate, ipdStatusBadgeVariante, taskStatusBadgeVariante } from '../utils/formatting'
import HudPanel from '../components/HudPanel'
import { OsdLogo } from '../components/OsdLogo'
import { ProgressRing } from '../components/ProgressRing'
import '../hud-home.css'

// Alle Daten, die das Dashboard braucht, in einem Objekt - so kann ich sie
// mit einem einzigen setState setzen und habe keinen "halb geladenen" Zustand.
interface DashboardData {
    tickets: TicketDto[]
    tasks: TaskDto[]
    checklists: ChecklistDto[]
    ipdDocuments: IpdDocumentDto[]
}

// Anteil der erledigten Items einer Checkliste in Prozent. Bei einer
// leeren Checkliste gebe ich 0 zurück, damit ich nicht durch 0 teile.
function checklistProgress(checklist: ChecklistDto): number {
    if (checklist.items.length === 0) return 0
    const done = checklist.items.filter((item) => item.done).length
    return (done / checklist.items.length) * 100
}

// Startseite: ohne Login ein Willkommens-Panel mit dem (einzigen) GitHub-
// Login-Button, mit Login ein Dashboard aus echten Daten der Anwendung.
export function Home() {
    const { username, loginUrl } = useAuth()
    // Hooks müssen vor jedem frühen return stehen (Rules of Hooks).
    const glpiUrl = useGlpiUrl(Boolean(username))
    const [data, setData] = useState<DashboardData | null>(null)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)

    // Dashboard-Daten laden, sobald jemand eingeloggt ist. Die vier Abfragen
    // laufen parallel (Promise.all), das ist schneller als nacheinander.
    // `abgebrochen` verhindert, dass ich State setze, wenn die Seite
    // inzwischen verlassen wurde.
    useEffect(() => {
        if (!username) return
        let aborted = false
        Promise.all([
            api.get<TicketDto[]>('/api/tickets'),
            api.get<TaskDto[]>('/api/tasks'),
            api.get<ChecklistDto[]>('/api/checklists'),
            api.get<IpdDocumentDto[]>('/api/ipd'),
        ])
            .then(([tickets, tasks, checklists, ipdDocuments]) => {
                if (!aborted) setData({ tickets, tasks, checklists, ipdDocuments })
            })
            .catch((error) => {
                console.error(error)
                if (!aborted) setErrorMessage('Die Dashboard-Daten konnten nicht geladen werden.')
            })
        return () => {
            aborted = true
        }
    }, [username])

    // ---------- Nicht eingeloggt: Willkommens-Panel ----------
    if (!username) {
        return (
            <div className="willkommen">
                <HudPanel title="Willkommen">
                    <div className="willkommen__inhalt">
                        <OsdLogo />
                        <p>
                            Support-Workflow und IPD-Dokumentgenerator für Wartungseinsätze:
                            Tickets aus GLPI, Aufgabenplanung, Checklisten und fertige
                            Dokumentation als PDF.
                        </p>
                        {/* Der einzige Login-Button der App. Er ist ein normaler Link,
                            weil GitHub den Nutzer per Browser-Weiterleitung anmeldet. */}
                        <a href={loginUrl} className="btn btn-primary btn-lg willkommen__login">
                            <FaGithub className="me-2" />
                            Mit GitHub anmelden
                        </a>
                    </div>
                </HudPanel>
            </div>
        )
    }

    if (errorMessage) {
        return <Alert variant="danger">{errorMessage}</Alert>
    }

    if (!data) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '40vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        )
    }

    // ---------- Kennzahlen berechnen ----------
    const { tickets, tasks, checklists, ipdDocuments } = data

    // Ein Ticket gilt als "offen", solange es nicht gelöst oder geschlossen ist.
    const openTickets = tickets.filter((t) => t.status !== 'SOLVED' && t.status !== 'CLOSED')
    const countNew = tickets.filter((t) => t.status === 'NEW').length
    const countInProgress = tickets.filter((t) => t.status === 'IN_PROGRESS').length
    const countPending = tickets.filter((t) => t.status === 'PENDING').length

    // Heutiges Datum als "YYYY-MM-DD" (Schwedisch liefert genau dieses Format).
    // Da das Zieldatum im selben Format kommt, kann ich beide als Text vergleichen.
    const today = new Date().toLocaleDateString('sv-SE')
    const openTasks = tasks
        .filter((t) => t.status !== 'DONE')
        // Tasks ohne Zieldatum ('') ans Ende, sonst nach Datum aufsteigend
        .sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'))
    const overdue = openTasks.filter((t) => t.dueDate !== null && t.dueDate < today)

    // Nur Checklisten, die noch nicht abgeschlossen sind, zeige ich als Ringe (max. 3).
    const activeChecklists = checklists.filter((c) => c.completedAt === null).slice(0, 3)

    // Die vier zuletzt bearbeiteten IPD-Dokumente.
    const latestDocuments = [...ipdDocuments]
        .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
        .slice(0, 4)

    return (
        <div className="dash">
            <h1 className="dash__titel">Dashboard</h1>
            <p className="dash__untertitel">Willkommen zurück, {username}</p>

            <div className="dash__grid">
                {/* ---------- Offene Tickets ---------- */}
                <HudPanel title="Offene Tickets" footerLink={{ to: '/tickets', label: 'Alle ansehen' }}>
                    <div className="dash__ticketzahl">
                        <div className="dash__grossezahl">{openTickets.length}</div>
                        <ul className="dash__legende">
                            <li><span className="punkt punkt--cyan" /> Neu <b>{countNew}</b></li>
                            <li><span className="punkt punkt--gold" /> In Bearbeitung <b>{countInProgress}</b></li>
                            <li><span className="punkt punkt--grau" /> Ausstehend <b>{countPending}</b></li>
                        </ul>
                    </div>
                </HudPanel>

                {/* ---------- Checklisten-Fortschritt ---------- */}
                <HudPanel title="Checklisten-Fortschritt" footerLink={{ to: '/checklists', label: 'Details' }}>
                    {activeChecklists.length === 0 ? (
                        <p className="dash__leer">Keine laufenden Checklisten.</p>
                    ) : (
                        <div className="dash__ringe">
                            {activeChecklists.map((c) => (
                                <ProgressRing key={c.id} label={c.title} percent={checklistProgress(c)} />
                            ))}
                        </div>
                    )}
                </HudPanel>

                {/* ---------- Überfällige / fällige Tasks ----------
                    Ton wird warnend (Gold) oder kritisch (Rot), wenn Tasks überfällig sind. */}
                <HudPanel
                    title="Fällige Tasks"
                    tone={overdue.length > 0 ? 'danger' : 'default'}
                    footerLink={{ to: '/tasks', label: 'Alle Tasks' }}
                >
                    {overdue.length > 0 && (
                        <p className="dash__warnung">{overdue.length} überfällig</p>
                    )}
                    {openTasks.length === 0 ? (
                        <p className="dash__leer">Keine offenen Tasks.</p>
                    ) : (
                        <ul className="dash__liste">
                            {openTasks.slice(0, 4).map((task) => (
                                <li key={task.id}>
                                    <div>
                                        <div className="dash__listentitel">{task.topic}</div>
                                        <Badge bg={taskStatusBadgeVariante(task.status)}>
                                            {TASK_STATUS_LABELS[task.status]}
                                        </Badge>
                                    </div>
                                    <div className={task.dueDate !== null && task.dueDate < today ? 'dash__datum dash__datum--rot' : 'dash__datum'}>
                                        {formatDueDate(task.dueDate)}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </HudPanel>

                {/* ---------- Letzte IPD-Dokumente ---------- */}
                <HudPanel title="Letzte IPD-Dokumente" className="dash__zweispaltig" footerLink={{ to: '/ipd', label: 'Alle Dokumente' }}>
                    {latestDocuments.length === 0 ? (
                        <p className="dash__leer">Noch keine Dokumente.</p>
                    ) : (
                        <ul className="dash__liste">
                            {latestDocuments.map((doc) => (
                                <li key={doc.id}>
                                    <div>
                                        <Link to={`/ipd/${doc.id}`} className="dash__listentitel">{doc.title}</Link>
                                        <Badge bg={ipdStatusBadgeVariante(doc.status)}>
                                            {IPD_DOCUMENT_STATUS_LABELS[doc.status]}
                                        </Badge>
                                    </div>
                                    <div className="dash__datum">{formatDate(doc.updatedAt)}</div>
                                </li>
                            ))}
                        </ul>
                    )}
                </HudPanel>

                {/* ---------- Schnellaktionen ---------- */}
                <HudPanel title="Schnellaktionen" className="dash__breit">
                    <div className="dash__aktionen">
                        <Link to="/tickets" className="dash__aktion">
                            <FaSyncAlt /> Ticket-Sync
                        </Link>
                        <Link to="/tasks" className="dash__aktion">
                            <FaTasks /> Task anlegen
                        </Link>
                        <Link to="/checklists" className="dash__aktion">
                            <FaClipboardCheck /> Checkliste aus Vorlage
                        </Link>
                        <Link to="/ipd" className="dash__aktion">
                            <FaFileAlt /> IPD erstellen
                        </Link>
                        {/* Externes GLPI: öffnet im neuen Tab */}
                        {glpiUrl && (
                            <a className="dash__aktion" href={glpiUrl} target="_blank" rel="noopener noreferrer">
                                <FaExternalLinkAlt /> GLPI öffnen
                            </a>
                        )}
                    </div>
                </HudPanel>
            </div>
        </div>
    )
}
