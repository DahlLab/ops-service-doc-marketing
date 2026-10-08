import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { currentLocale } from '../i18n'

export function HudClock() {
    const { t } = useTranslation()
    const [now, setNow] = useState(() => new Date())

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000)
        return () => clearInterval(timer)
    }, [])

    return (
        <div className="hud-clock" aria-label={t('clock.label')}>
            <span className="hud-clock__time">
                {now.toLocaleTimeString(currentLocale())}
            </span>
            <span className="hud-clock__date">
                {now.toLocaleDateString(currentLocale(), { day: '2-digit', month: '2-digit', year: 'numeric' })}
            </span>
        </div>
    )
}
