import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dropdown } from 'react-bootstrap'
import { FaGithub, FaSignOutAlt } from 'react-icons/fa'

interface UserMenuProps {
    username: string
    onLogout: () => void

    drop?: 'up' | 'down'
}

export function UserMenu({ username, onLogout, drop = 'down' }: Readonly<UserMenuProps>) {
    const { t } = useTranslation()
    const [avatarError, setAvatarError] = useState(false)

    return (
        <Dropdown drop={drop} align="end" className="user-menu">

            <Dropdown.Toggle as="button" className="user-menu__toggle" aria-label={t('user.menu')}>
                {avatarError ? (
                    <span className="user-menu__avatar user-menu__avatar--fallback">
                        <FaGithub />
                    </span>
                ) : (
                    <img
                        className="user-menu__avatar"
                        src={`https://github.com/${encodeURIComponent(username)}.png?size=80`}
                        alt=""
                        onError={() => setAvatarError(true)}
                    />
                )}
                <span className="user-menu__text">
                    <span className="user-menu__name">{username}</span>
                    <span className="user-menu__role">{t('user.signedIn')}</span>
                </span>
            </Dropdown.Toggle>

            <Dropdown.Menu variant="dark">
                <Dropdown.Item as="button" onClick={onLogout}>
                    <FaSignOutAlt className="me-2" />
                    {t('user.signOut')}
                </Dropdown.Item>
            </Dropdown.Menu>
        </Dropdown>
    )
}
