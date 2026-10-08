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
import { formatiereDatum, formatiereZieldatum, ipdStatusBadgeVariante, taskStatusBadgeVariante } from '../utils/formatierung'
import HudPanel from '../components/HudPanel'
import { OsdLogo } from '../components/OsdLogo'
import { ProgressRing } from '../components/ProgressRing'
import '../hud-home.css'

// Alle Daten, die das Dashboard braucht, in einem Objekt - so kann ich sie
// mit einem einzigen setState setzen und habe keinen "halb geladenen" Zustand.
interface DashboardDaten {
    tickets: TicketDto[]
    tasks: TaskDto[]
    checklisten: ChecklistDto[]
    dokumente: IpdDocumentDto[]
}

// Anteil der erledigten Items einer Checkliste in Prozent. Bei einer
// leeren Checkliste gebe ich 0 zurück, damit ich nicht durch 0 teile.
function checklistFortschritt(checkliste: ChecklistDto): number {
    if (checkliste.items.length === 0) return 0
    const erledigt = checkliste.items.filter((item) => item.erledigt).length
    return (erledigt / checkliste.items.length) * 100
}

// Startseite: ohne Login ein Willkommens-Panel mit dem (einzigen) GitHub-
// Login-Button, mit Login ein Dashboard aus echten Daten der Anwendung.
export function Home() {
    const { username, loginUrl } = useAuth()
    // Hooks müssen vor jedem frühen return stehen (Rules of Hooks).
    const glpiUrl = useGlpiUrl(Boolean(username))
    const [daten, setDaten] = useState<DashboardDaten | null>(null)
    const [fehler, setFehler] = useState<string | null>(null)

    // Dashboard-Daten laden, sobald jemand eingeloggt ist. Die vier Abfragen
    // laufen parallel (Promise.all), das ist schneller als nacheinander.
    // `abgebrochen` verhindert, dass ich State setze, wenn die Seite
    // inzwischen verlassen wurde.
    useEffect(() => {
        if (!username) return
        let abgebrochen = false
        Promise.all([
            api.get<TicketDto[]>('/api/tickets'),
            api.get<TaskDto[]>('/api/tasks'),
            api.get<ChecklistDto[]>('/api/checklists'),
            api.get<IpdDocumentDto[]>('/api/ipd'),
        ])
            .then(([tickets, tasks, checklisten, dokumente]) => {
                if (!abgebrochen) setDaten({ tickets, tasks, checklisten, dokumente })
            })
            .catch((error) => {
                console.error(error)
                if (!abgebrochen) setFehler('Die Dashboard-Daten konnten nicht geladen werden.')
            })
        return () => {
            abgebrochen = true
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

    if (fehler) {
        return <Alert variant="danger">{fehler}</Alert>
    }

    if (!daten) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '40vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        )
    }

    // ---------- Kennzahlen berechnen ----------
    const { tickets, tasks, checklisten, dokumente } = daten

    // Ein Ticket gilt als "offen", solange es nicht gelöst oder geschlossen ist.
    const offeneTickets = tickets.filter((t) => t.status !== 'GELOEST' && t.status !== 'GESCHLOSSEN')
    const anzahlNeu = tickets.filter((t) => t.status === 'NEU').length
    const anzahlInArbeit = tickets.filter((t) => t.status === 'IN_BEARBEITUNG').length
    const anzahlAusstehend = tickets.filter((t) => t.status === 'AUSSTEHEND').length

    // Heutiges Datum als "YYYY-MM-DD" (Schwedisch liefert genau dieses Format).
    // Da das Zieldatum im selben Format kommt, kann ich beide als Text vergleichen.
    const heute = new Date().toLocaleDateString('sv-SE')
    const offeneTasks = tasks
        .filter((t) => t.status !== 'ERLEDIGT')
        // Tasks ohne Zieldatum ('') ans Ende, sonst nach Datum aufsteigend
        .sort((a, b) => (a.zieldatum ?? '9999').localeCompare(b.zieldatum ?? '9999'))
    const ueberfaellig = offeneTasks.filter((t) => t.zieldatum !== null && t.zieldatum < heute)

    // Nur Checklisten, die noch nicht abgeschlossen sind, zeige ich als Ringe (max. 3).
    const laufendeChecklisten = checklisten.filter((c) => c.abgeschlossenAm === null).slice(0, 3)

    // Die vier zuletzt bearbeiteten IPD-Dokumente.
    const letzteDokumente = [...dokumente]
        .sort((a, b) => b.aktualisiertAm.localeCompare(a.aktualisiertAm))
        .slice(0, 4)

    return (
        <div className="dash">
            <h1 className="dash__titel">Dashboard</h1>
            <p className="dash__untertitel">Willkommen zurück, {username}</p>

            <div className="dash__grid">
                {/* ---------- Offene Tickets ---------- */}
                <HudPanel title="Offene Tickets" footerLink={{ to: '/tickets', label: 'Alle ansehen' }}>
                    <div className="dash__ticketzahl">
                        <div className="dash__grossezahl">{offeneTickets.length}</div>
                        <ul className="dash__legende">
                            <li><span className="punkt punkt--cyan" /> Neu <b>{anzahlNeu}</b></li>
                            <li><span className="punkt punkt--gold" /> In Bearbeitung <b>{anzahlInArbeit}</b></li>
                            <li><span className="punkt punkt--grau" /> Ausstehend <b>{anzahlAusstehend}</b></li>
                        </ul>
                    </div>
                </HudPanel>

                {/* ---------- Checklisten-Fortschritt ---------- */}
                <HudPanel title="Checklisten-Fortschritt" footerLink={{ to: '/checklisten', label: 'Details' }}>
                    {laufendeChecklisten.length === 0 ? (
                        <p className="dash__leer">Keine laufenden Checklisten.</p>
                    ) : (
                        <div className="dash__ringe">
                            {laufendeChecklisten.map((c) => (
                                <ProgressRing key={c.id} label={c.titel} prozent={checklistFortschritt(c)} />
                            ))}
                        </div>
                    )}
                </HudPanel>

                {/* ---------- Überfällige / fällige Tasks ----------
                    Ton wird warnend (Gold) oder kritisch (Rot), wenn Tasks überfällig sind. */}
                <HudPanel
                    title="Fällige Tasks"
                    tone={ueberfaellig.length > 0 ? 'danger' : 'default'}
                    footerLink={{ to: '/tasks', label: 'Alle Tasks' }}
                >
                    {ueberfaellig.length > 0 && (
                        <p className="dash__warnung">{ueberfaellig.length} überfällig</p>
                    )}
                    {offeneTasks.length === 0 ? (
                        <p className="dash__leer">Keine offenen Tasks.</p>
                    ) : (
                        <ul className="dash__liste">
                            {offeneTasks.slice(0, 4).map((task) => (
                                <li key={task.id}>
                                    <div>
                                        <div className="dash__listentitel">{task.thema}</div>
                                        <Badge bg={taskStatusBadgeVariante(task.status)}>
                                            {TASK_STATUS_LABELS[task.status]}
                                        </Badge>
                                    </div>
                                    <div className={task.zieldatum !== null && task.zieldatum < heute ? 'dash__datum dash__datum--rot' : 'dash__datum'}>
                                        {formatiereZieldatum(task.zieldatum)}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </HudPanel>

                {/* ---------- Letzte IPD-Dokumente ---------- */}
                <HudPanel title="Letzte IPD-Dokumente" className="dash__zweispaltig" footerLink={{ to: '/ipd', label: 'Alle Dokumente' }}>
                    {letzteDokumente.length === 0 ? (
                        <p className="dash__leer">Noch keine Dokumente.</p>
                    ) : (
                        <ul className="dash__liste">
                            {letzteDokumente.map((dok) => (
                                <li key={dok.id}>
                                    <div>
                                        <Link to={`/ipd/${dok.id}`} className="dash__listentitel">{dok.titel}</Link>
                                        <Badge bg={ipdStatusBadgeVariante(dok.status)}>
                                            {IPD_DOCUMENT_STATUS_LABELS[dok.status]}
                                        </Badge>
                                    </div>
                                    <div className="dash__datum">{formatiereDatum(dok.aktualisiertAm)}</div>
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
                        <Link to="/checklisten" className="dash__aktion">
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
