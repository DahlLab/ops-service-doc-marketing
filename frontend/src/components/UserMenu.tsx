import { useState } from 'react'
import { Dropdown } from 'react-bootstrap'
import { FaGithub, FaSignOutAlt } from 'react-icons/fa'

/**
 * UserMenu - Avatar + GitHub-Username mit Dropdown "Abmelden".
 *
 * Das Profilbild hole ich direkt von GitHub (https://github.com/<name>.png),
 * dafür brauche ich keinen Backend-Aufruf. Lädt das Bild nicht (z. B.
 * offline), zeige ich stattdessen das GitHub-Icon.
 */
interface UserMenuProps {
    username: string
    onLogout: () => void
    /** In welche Richtung das Menü aufklappt (in der Sidebar unten: nach oben) */
    drop?: 'up' | 'down'
}

// Readonly<...> markiert die Props als schreibgeschützt - ich darf sie in der Komponente nicht ändern
export function UserMenu({ username, onLogout, drop = 'down' }: Readonly<UserMenuProps>) {
    // Merkt sich, ob das Avatar-Bild nicht geladen werden konnte
    const [avatarFehler, setAvatarFehler] = useState(false)

    return (
        <Dropdown drop={drop} align="end" className="user-menu">
            {/* Eigener Toggle als Button, damit ich Avatar + Name selbst gestalten kann */}
            <Dropdown.Toggle as="button" className="user-menu__toggle" aria-label="Benutzermenü">
                {avatarFehler ? (
                    <span className="user-menu__avatar user-menu__avatar--fallback">
                        <FaGithub />
                    </span>
                ) : (
                    <img
                        className="user-menu__avatar"
                        src={`https://github.com/${encodeURIComponent(username)}.png?size=80`}
                        alt=""
                        onError={() => setAvatarFehler(true)}
                    />
                )}
                <span className="user-menu__text">
                    <span className="user-menu__name">{username}</span>
                    <span className="user-menu__rolle">Angemeldet</span>
                </span>
            </Dropdown.Toggle>

            <Dropdown.Menu variant="dark">
                <Dropdown.Item as="button" onClick={onLogout}>
                    <FaSignOutAlt className="me-2" />
                    Abmelden
                </Dropdown.Item>
            </Dropdown.Menu>
        </Dropdown>
    )
}
