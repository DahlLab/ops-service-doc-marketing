import { useEffect, useState } from 'react'

export function HudClock() {
    const [now, setNow] = useState(() => new Date())

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000)
        return () => clearInterval(timer)
    }, [])

    return (
        <div className="hud-clock" aria-label="Aktuelle Uhrzeit und Datum">
            <span className="hud-clock__time">
                {now.toLocaleTimeString('de-DE')}
            </span>
            <span className="hud-clock__date">
                {now.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' })}
            </span>
        </div>
    )
}
