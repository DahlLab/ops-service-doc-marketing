import { useEffect, useState } from 'react'

/**
 * HudUhr - zeigt Uhrzeit und Datum live an (wie in der Kopfleiste des Mockups).
 *
 * Ich halte die aktuelle Zeit im State und aktualisiere sie jede Sekunde
 * per setInterval. Beim Verlassen der Seite räumt die Cleanup-Funktion
 * den Timer wieder auf, sonst würde er im Hintergrund weiterlaufen.
 */
export function HudClock() {
    // Funktion als Startwert: wird nur beim ersten Rendern ausgeführt
    const [now, setNow] = useState(() => new Date())

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000)
        return () => clearInterval(timer)
    }, [])

    return (
        <div className="hud-uhr" aria-label="Aktuelle Uhrzeit und Datum">
            <span className="hud-uhr__zeit">
                {now.toLocaleTimeString('de-DE')}
            </span>
            <span className="hud-uhr__datum">
                {now.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' })}
            </span>
        </div>
    )
}
