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

interface DashboardData {
    tickets: TicketDto[]
    tasks: TaskDto[]
    checklists: ChecklistDto[]
    ipdDocuments: IpdDocumentDto[]
}

function checklistProgress(checklist: ChecklistDto): number {
    if (checklist.items.length === 0) return 0
    const done = checklist.items.filter((item) => item.done).length
    return (done / checklist.items.length) * 100
}

export function Home() {
    const { username, loginUrl } = useAuth()

    const glpiUrl = useGlpiUrl(Boolean(username))
    const [data, setData] = useState<DashboardData | null>(null)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)

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

    if (!username) {
        return (
            <div className="welcome">
                <HudPanel title="Willkommen">
                    <div className="welcome__content">
                        <OsdLogo />
                        <p>
                            Support-Workflow und IPD-Dokumentgenerator für Wartungseinsätze:
                            Tickets aus GLPI, Aufgabenplanung, Checklisten und fertige
                            Dokumentation als PDF.
                        </p>

                        <a href={loginUrl} className="btn btn-primary btn-lg welcome__login">
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

    const { tickets, tasks, checklists, ipdDocuments } = data

    const openTickets = tickets.filter((t) => t.status !== 'SOLVED' && t.status !== 'CLOSED')
    const countNew = tickets.filter((t) => t.status === 'NEW').length
    const countInProgress = tickets.filter((t) => t.status === 'IN_PROGRESS').length
    const countPending = tickets.filter((t) => t.status === 'PENDING').length

    const today = new Date().toLocaleDateString('sv-SE')
    const openTasks = tasks
        .filter((t) => t.status !== 'DONE')
        .sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'))
    const overdue = openTasks.filter((t) => t.dueDate !== null && t.dueDate < today)

    const activeChecklists = checklists.filter((c) => c.completedAt === null).slice(0, 3)

    const latestDocuments = [...ipdDocuments]
        .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
        .slice(0, 4)

    return (
        <div className="dash">
            <h1 className="dash__title">Dashboard</h1>
            <p className="dash__subtitle">Willkommen zurück, {username}</p>

            <div className="dash__grid">

                <HudPanel title="Offene Tickets" footerLink={{ to: '/tickets', label: 'Alle ansehen' }}>
                    <div className="dash__ticketcount">
                        <div className="dash__bignumber">{openTickets.length}</div>
                        <ul className="dash__legend">
                            <li><span className="dot dot--cyan" /> Neu <b>{countNew}</b></li>
                            <li><span className="dot dot--gold" /> In Bearbeitung <b>{countInProgress}</b></li>
                            <li><span className="dot dot--gray" /> Ausstehend <b>{countPending}</b></li>
                        </ul>
                    </div>
                </HudPanel>

                <HudPanel title="Checklisten-Fortschritt" footerLink={{ to: '/checklists', label: 'Details' }}>
                    {activeChecklists.length === 0 ? (
                        <p className="dash__empty">Keine laufenden Checklisten.</p>
                    ) : (
                        <div className="dash__rings">
                            {activeChecklists.map((c) => (
                                <ProgressRing key={c.id} label={c.title} percent={checklistProgress(c)} />
                            ))}
                        </div>
                    )}
                </HudPanel>

                <HudPanel
                    title="Fällige Tasks"
                    tone={overdue.length > 0 ? 'danger' : 'default'}
                    footerLink={{ to: '/tasks', label: 'Alle Tasks' }}
                >
                    {overdue.length > 0 && (
                        <p className="dash__warning">{overdue.length} überfällig</p>
                    )}
                    {openTasks.length === 0 ? (
                        <p className="dash__empty">Keine offenen Tasks.</p>
                    ) : (
                        <ul className="dash__list">
                            {openTasks.slice(0, 4).map((task) => (
                                <li key={task.id}>
                                    <div>
                                        <div className="dash__listtitle">{task.topic}</div>
                                        <Badge bg={taskStatusBadgeVariante(task.status)}>
                                            {TASK_STATUS_LABELS[task.status]}
                                        </Badge>
                                    </div>
                                    <div className={task.dueDate !== null && task.dueDate < today ? 'dash__date dash__date--red' : 'dash__date'}>
                                        {formatDueDate(task.dueDate)}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </HudPanel>

                <HudPanel title="Letzte IPD-Dokumente" className="dash__twocolumn" footerLink={{ to: '/ipd', label: 'Alle Dokumente' }}>
                    {latestDocuments.length === 0 ? (
                        <p className="dash__empty">Noch keine Dokumente.</p>
                    ) : (
                        <ul className="dash__list">
                            {latestDocuments.map((doc) => (
                                <li key={doc.id}>
                                    <div>
                                        <Link to={`/ipd/${doc.id}`} className="dash__listtitle">{doc.title}</Link>
                                        <Badge bg={ipdStatusBadgeVariante(doc.status)}>
                                            {IPD_DOCUMENT_STATUS_LABELS[doc.status]}
                                        </Badge>
                                    </div>
                                    <div className="dash__date">{formatDate(doc.updatedAt)}</div>
                                </li>
                            ))}
                        </ul>
                    )}
                </HudPanel>

                <HudPanel title="Schnellaktionen" className="dash__wide">
                    <div className="dash__actions">
                        <Link to="/tickets" className="dash__action">
                            <FaSyncAlt /> Ticket-Sync
                        </Link>
                        <Link to="/tasks" className="dash__action">
                            <FaTasks /> Task anlegen
                        </Link>
                        <Link to="/checklists" className="dash__action">
                            <FaClipboardCheck /> Checkliste aus Vorlage
                        </Link>
                        <Link to="/ipd" className="dash__action">
                            <FaFileAlt /> IPD erstellen
                        </Link>

                        {glpiUrl && (
                            <a className="dash__action" href={glpiUrl} target="_blank" rel="noopener noreferrer">
                                <FaExternalLinkAlt /> GLPI öffnen
                            </a>
                        )}
                    </div>
                </HudPanel>
            </div>
        </div>
    )
}
