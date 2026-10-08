import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
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
import { LanguageSwitcher } from './LanguageSwitcher'
import '../hud-layout.css'

const MENU = [
    { to: '/', labelKey: 'nav.dashboard', icon: <FaTachometerAlt />, end: true },
    { to: '/tickets', labelKey: 'nav.tickets', icon: <FaTicketAlt />, end: false },
    { to: '/tasks', labelKey: 'nav.tasks', icon: <FaTasks />, end: false },
    { to: '/checklists', labelKey: 'nav.checklists', icon: <FaClipboardCheck />, end: false },
    { to: '/ipd', labelKey: 'nav.ipd', icon: <FaFileAlt />, end: false },
]

export function Layout() {
    const { t } = useTranslation()
    const { username, loading, logout } = useAuth()
    const glpiUrl = useGlpiUrl(Boolean(username))

    const [menuOpen, setMenuOpen] = useState(false)

    const dialogRef = useRef<HTMLDialogElement>(null)

    useEffect(() => {
        const dialog = dialogRef.current
        if (menuOpen && dialog && !dialog.open) {
            dialog.showModal()
        }
    }, [menuOpen])

    const loggedIn = !loading && Boolean(username)

    return (
        <div className={`hud-shell ${loggedIn ? 'hud-shell--with-sidebar' : ''}`}>

            {loggedIn && (
                <aside className="hud-sidebar">
                    <Link to="/" className="hud-sidebar__logo" aria-label={t('nav.home')}>
                        <OsdLogo />
                    </Link>

                    <nav className="hud-nav" aria-label={t('nav.main')}>
                        {MENU.map((entry) => (
                            <NavLink
                                key={entry.to}
                                to={entry.to}
                                end={entry.end}
                                className={({ isActive }) => `hud-nav__item ${isActive ? 'is-active' : ''}`}
                            >
                                {entry.icon}
                                <span>{t(entry.labelKey)}</span>
                            </NavLink>
                        ))}

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

                <header className="hud-topbar">

                    {loggedIn && (
                        <button
                            type="button"
                            className="hud-topbar__burger"
                            aria-label={t('nav.openMenu')}
                            onClick={() => setMenuOpen(true)}
                        >
                            <FaBars />
                        </button>
                    )}

                    <Link to="/" className="hud-topbar__logo" aria-label={t('nav.home')}>
                        <OsdLogo />
                    </Link>
                    <div className="hud-topbar__spacer" />
                    <LanguageSwitcher />
                    <HudClock />

                    {loggedIn && username && (
                        <div className="hud-topbar__user">
                            <UserMenu username={username} onLogout={logout} />
                        </div>
                    )}
                </header>

                <main className="hud-content">
                    <Outlet />
                </main>

                <footer className="hud-statusbar">
                    <span>OpsServiceDoc v1.0</span>
                    <span className="hud-statusbar__center">
                        {loggedIn ? t('statusbar.secure') : t('statusbar.notSignedIn')}
                    </span>
                    <span>{loggedIn ? username : ''}</span>
                </footer>
            </div>

            {loggedIn && menuOpen && (
                <dialog
                    ref={dialogRef}
                    className="hud-overlay"
                    aria-label={t('nav.menu')}

                    onClose={() => setMenuOpen(false)}
                >
                    <div className="hud-overlay__header">
                        <OsdLogo />
                        <button
                            type="button"
                            className="hud-topbar__burger"
                            aria-label={t('nav.closeMenu')}
                            onClick={() => setMenuOpen(false)}
                        >
                            <FaTimes />
                        </button>
                    </div>
                    <div className="hud-overlay__tiles">
                        {MENU.map((entry) => (
                            <NavLink
                                key={entry.to}
                                to={entry.to}
                                end={entry.end}
                                className={({ isActive }) => `hud-tile ${isActive ? 'is-active' : ''}`}

                                onClick={() => setMenuOpen(false)}
                            >
                                {entry.icon}
                                <span>{t(entry.labelKey)}</span>
                            </NavLink>
                        ))}
                        {glpiUrl && (
                            <a
                                className="hud-tile"
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
