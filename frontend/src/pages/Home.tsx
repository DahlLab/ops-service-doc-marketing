import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
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
import { formatDate, formatDueDate, ipdStatusBadgeVariant, taskStatusBadgeVariant } from '../utils/formatting'
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
    const { t } = useTranslation('home')
    const { username, loginUrl } = useAuth()

    const glpiUrl = useGlpiUrl(Boolean(username))
    const [data, setData] = useState<DashboardData | null>(null)
    const [hasError, setHasError] = useState(false)

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
                if (!aborted) setHasError(true)
            })
        return () => {
            aborted = true
        }
    }, [username])

    if (!username) {
        return (
            <div className="welcome">
                <HudPanel title={t('welcome.title')}>
                    <div className="welcome__content">
                        <OsdLogo />
                        <p>
                            {t('welcome.description')}
                        </p>

                        <a href={loginUrl} className="btn btn-primary btn-lg welcome__login">
                            <FaGithub className="me-2" />
                            {t('welcome.login')}
                        </a>
                    </div>
                </HudPanel>
            </div>
        )
    }

    if (hasError) {
        return <Alert variant="danger">{t('error.loadFailed')}</Alert>
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
            <h1 className="dash__title">{t('title')}</h1>
            <p className="dash__subtitle">{t('subtitle', { name: username })}</p>

            <div className="dash__grid">

                <HudPanel title={t('tickets.title')} footerLink={{ to: '/tickets', label: t('tickets.viewAll') }}>
                    <div className="dash__ticketcount">
                        <div className="dash__bignumber">{openTickets.length}</div>
                        <ul className="dash__legend">
                            <li><span className="dot dot--cyan" /> {t('status.ticket.NEW', { ns: 'common' })} <b>{countNew}</b></li>
                            <li><span className="dot dot--gold" /> {t('status.ticket.IN_PROGRESS', { ns: 'common' })} <b>{countInProgress}</b></li>
                            <li><span className="dot dot--gray" /> {t('status.ticket.PENDING', { ns: 'common' })} <b>{countPending}</b></li>
                        </ul>
                    </div>
                </HudPanel>

                <HudPanel title={t('checklists.title')} footerLink={{ to: '/checklists', label: t('checklists.details') }}>
                    {activeChecklists.length === 0 ? (
                        <p className="dash__empty">{t('checklists.empty')}</p>
                    ) : (
                        <div className="dash__rings">
                            {activeChecklists.map((c) => (
                                <ProgressRing key={c.id} label={c.title} percent={checklistProgress(c)} />
                            ))}
                        </div>
                    )}
                </HudPanel>

                <HudPanel
                    title={t('tasks.title')}
                    tone={overdue.length > 0 ? 'danger' : 'default'}
                    footerLink={{ to: '/tasks', label: t('tasks.viewAll') }}
                >
                    {overdue.length > 0 && (
                        <p className="dash__warning">{t('tasks.overdue', { count: overdue.length })}</p>
                    )}
                    {openTasks.length === 0 ? (
                        <p className="dash__empty">{t('tasks.empty')}</p>
                    ) : (
                        <ul className="dash__list">
                            {openTasks.slice(0, 4).map((task) => (
                                <li key={task.id}>
                                    <div>
                                        <div className="dash__listtitle">{task.topic}</div>
                                        <Badge bg={taskStatusBadgeVariant(task.status)}>
                                            {t(`status.task.${task.status}`, { ns: 'common' })}
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

                <HudPanel title={t('ipd.title')} className="dash__twocolumn" footerLink={{ to: '/ipd', label: t('ipd.viewAll') }}>
                    {latestDocuments.length === 0 ? (
                        <p className="dash__empty">{t('ipd.empty')}</p>
                    ) : (
                        <ul className="dash__list">
                            {latestDocuments.map((doc) => (
                                <li key={doc.id}>
                                    <div>
                                        <Link to={`/ipd/${doc.id}`} className="dash__listtitle">{doc.title}</Link>
                                        <Badge bg={ipdStatusBadgeVariant(doc.status)}>
                                            {t(`status.ipd.${doc.status}`, { ns: 'common' })}
                                        </Badge>
                                    </div>
                                    <div className="dash__date">{formatDate(doc.updatedAt)}</div>
                                </li>
                            ))}
                        </ul>
                    )}
                </HudPanel>

                <HudPanel title={t('actions.title')} className="dash__wide">
                    <div className="dash__actions">
                        <Link to="/tickets" className="dash__action">
                            <FaSyncAlt /> {t('actions.ticketSync')}
                        </Link>
                        <Link to="/tasks" className="dash__action">
                            <FaTasks /> {t('actions.createTask')}
                        </Link>
                        <Link to="/checklists" className="dash__action">
                            <FaClipboardCheck /> {t('actions.checklistFromTemplate')}
                        </Link>
                        <Link to="/ipd" className="dash__action">
                            <FaFileAlt /> {t('actions.createIpd')}
                        </Link>

                        {glpiUrl && (
                            <a className="dash__action" href={glpiUrl} target="_blank" rel="noopener noreferrer">
                                <FaExternalLinkAlt /> {t('actions.openGlpi')}
                            </a>
                        )}
                    </div>
                </HudPanel>
            </div>
        </div>
    )
}
