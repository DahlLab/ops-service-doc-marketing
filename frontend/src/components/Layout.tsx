import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, Link } from 'react-router-dom'
import {
    FaBars,
    FaClipboardCheck,
    FaExternalLinkAlt,
    FaFileAlt,
    FaTasks,
    FaTachometerAlt,
    FaTicketAlt,
    FaTimes,
} from 'react-icons/fa'
import { useAuth } from '../auth/useAuth'
import { useGlpiUrl } from '../hooks/useGlpiUrl'
import { OsdLogo } from './OsdLogo'
import { HudClock } from './HudClock'
import { UserMenu } from './UserMenu'
import '../hud-layout.css'

// Alle internen Menüpunkte an EINER Stelle. Sidebar und mobiles Kachelmenü
// lesen aus derselben Liste, so müssen neue Seiten nur einmal eingetragen
// werden. `end` sorgt dafür, dass "/" nur bei der Startseite aktiv ist und
// nicht bei jeder Unterseite.
const MENU = [
    { to: '/', label: 'Dashboard', icon: <FaTachometerAlt />, end: true },
    { to: '/tickets', label: 'Tickets', icon: <FaTicketAlt />, end: false },
    { to: '/tasks', label: 'TaskPlanner', icon: <FaTasks />, end: false },
    { to: '/checklists', label: 'Checklisten', icon: <FaClipboardCheck />, end: false },
    { to: '/ipd', label: 'IPD-Generator', icon: <FaFileAlt />, end: false },
]

// Gemeinsames Grundgerüst für alle Seiten (siehe App.tsx):
// - Desktop: feste Sidebar links, oben eine Kopfleiste mit Uhr, unten eine Statusleiste
// - Mobil: Kopfzeile mit Hamburger, der ein Vollbild-Kachelmenü öffnet
// Die eigentliche Seite wird über <Outlet /> in den Hauptbereich gerendert.
export function Layout() {
    const { username, loading, logout } = useAuth()
    const glpiUrl = useGlpiUrl(Boolean(username))
    // Steuert das mobile Vollbild-Menü
    const [menuOpen, setMenuOpen] = useState(false)
    // Verweis auf das <dialog>-Element, damit ich es per showModal() öffnen kann
    const dialogRef = useRef<HTMLDialogElement>(null)

    // Sobald das Menü offen ist und das <dialog> gerendert wurde, öffne ich es als echtes
    // modales Dialogfenster. Der Browser übernimmt dann Fokus-Falle, Esc-Taste und die
    // Screenreader-Rolle (deshalb brauche ich weder role="dialog" noch aria-modal).
    // Die Prüfung auf dialog.open verhindert einen Fehler, falls der Effect im
    // StrictMode zweimal läuft.
    useEffect(() => {
        const dialog = dialogRef.current
        if (menuOpen && dialog && !dialog.open) {
            dialog.showModal()
        }
    }, [menuOpen])

    // Die Navigation zeige ich nur, wenn jemand eingeloggt ist - ohne Login
    // würden die Links ohnehin nur in einen 401 laufen (siehe SecurityConfig).
    const loggedIn = !loading && Boolean(username)

    return (
        <div className={`hud-shell ${loggedIn ? 'hud-shell--mit-sidebar' : ''}`}>
            {/* ---------- Sidebar (nur Desktop, nur eingeloggt) ---------- */}
            {loggedIn && (
                <aside className="hud-sidebar">
                    <Link to="/" className="hud-sidebar__logo" aria-label="Zur Startseite">
                        <OsdLogo />
                    </Link>

                    <nav className="hud-nav" aria-label="Hauptnavigation">
                        {MENU.map((entry) => (
                            <NavLink
                                key={entry.to}
                                to={entry.to}
                                end={entry.end}
                                className={({ isActive }) => `hud-nav__item ${isActive ? 'is-active' : ''}`}
                            >
                                {entry.icon}
                                <span>{entry.label}</span>
                            </NavLink>
                        ))}
                        {/* GLPI ist ein externes System: eigener Tab, daher normaler <a>-Link
                            und kein Router-Link. rel="noopener noreferrer" verhindert, dass
                            die geöffnete Seite Zugriff auf mein Fenster bekommt. */}
                        {glpiUrl && (
                            <a
                                className="hud-nav__item"
                                href={glpiUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                <FaExternalLinkAlt />
                                <span>GLPI ↗</span>
                            </a>
                        )}
                    </nav>
                </aside>
            )}

            <div className="hud-main">
                {/* ---------- Kopfleiste ---------- */}
                <header className="hud-topbar">
                    {/* Mobil: Hamburger links */}
                    {loggedIn && (
                        <button
                            type="button"
                            className="hud-topbar__burger"
                            aria-label="Menü öffnen"
                            onClick={() => setMenuOpen(true)}
                        >
                            <FaBars />
                        </button>
                    )}
                    {/* Das Logo in der Kopfleiste zeige ich nur mobil bzw. ohne Sidebar,
                        sonst steht es doppelt (die Sidebar hat ihr eigenes). */}
                    <Link to="/" className="hud-topbar__logo" aria-label="Zur Startseite">
                        <OsdLogo />
                    </Link>
                    <div className="hud-topbar__spacer" />
                    <HudClock />
                    {/* Das Benutzermenü sitzt oben rechts in der Kopfleiste, neben der Uhr */}
                    {loggedIn && username && (
                        <div className="hud-topbar__user">
                            <UserMenu username={username} onLogout={logout} />
                        </div>
                    )}
                </header>

                <main className="hud-content">
                    <Outlet />
                </main>

                {/* ---------- Statusleiste ---------- */}
                <footer className="hud-statusbar">
                    <span>OpsServiceDoc v1.0</span>
                    <span className="hud-statusbar__mitte">
                        {loggedIn ? 'Gesicherte Verbindung · Session aktiv' : 'Nicht angemeldet'}
                    </span>
                    <span>{loggedIn ? username : ''}</span>
                </footer>
            </div>

            {/* ---------- Mobiles Vollbild-Menü (Kacheln) ---------- */}
            {loggedIn && menuOpen && (
                <dialog
                    ref={dialogRef}
                    className="hud-overlay"
                    aria-label="Menü"
                    // Esc schließt ein modales <dialog> von selbst - dann muss ich meinen
                    // State nachziehen, sonst wäre menueOffen weiterhin true.
                    onClose={() => setMenuOpen(false)}
                >
                    <div className="hud-overlay__kopf">
                        <OsdLogo />
                        <button
                            type="button"
                            className="hud-topbar__burger"
                            aria-label="Menü schließen"
                            onClick={() => setMenuOpen(false)}
                        >
                            <FaTimes />
                        </button>
                    </div>
                    <div className="hud-overlay__kacheln">
                        {MENU.map((entry) => (
                            <NavLink
                                key={entry.to}
                                to={entry.to}
                                end={entry.end}
                                className={({ isActive }) => `hud-kachel ${isActive ? 'is-active' : ''}`}
                                // Nach dem Klick Menü schließen, sonst läge es über der neuen Seite
                                onClick={() => setMenuOpen(false)}
                            >
                                {entry.icon}
                                <span>{entry.label}</span>
                            </NavLink>
                        ))}
                        {glpiUrl && (
                            <a
                                className="hud-kachel"
                                href={glpiUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                onClick={() => setMenuOpen(false)}
                            >
                                <FaExternalLinkAlt />
                                <span>GLPI ↗</span>
                            </a>
                        )}
                    </div>
                </dialog>
            )}
        </div>
    )
}
