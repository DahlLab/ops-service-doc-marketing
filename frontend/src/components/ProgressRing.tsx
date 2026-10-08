/**
 * ProgressRing - Kreis-Anzeige für einen Prozentwert (wie "CPU 43 %" im Mockup).
 *
 * Technik: Ein SVG-Kreis mit gestricheltem Rand. `stroke-dasharray` legt
 * fest, wie lang der sichtbare Strich im Verhältnis zum Kreisumfang ist -
 * so zeichne ich genau den Anteil `prozent` des Rings.
 */
interface ProgressRingProps {
    /** Wert von 0 bis 100 */
    prozent: number
    /** Text über dem Wert, z. B. der Name der Checkliste */
    label: string
    /** Farbe des Fortschrittsbogens */
    ton?: 'cyan' | 'gold' | 'rot'
}

// Radius des Kreises im SVG-Koordinatensystem (viewBox 100 x 100)
const RADIUS = 42
// Umfang = 2 * pi * r - die Länge, die 100 % entspricht
const UMFANG = 2 * Math.PI * RADIUS

// Readonly<...> markiert die Props als schreibgeschützt - ich darf sie in der Komponente nicht ändern
export function ProgressRing({ prozent, label, ton = 'cyan' }: Readonly<ProgressRingProps>) {
    // Wert auf 0..100 begrenzen, damit ein Datenfehler den Ring nicht kaputt macht
    const wert = Math.max(0, Math.min(100, Math.round(prozent)))
    // So viel vom Umfang soll gefüllt sein
    const gefuellt = (wert / 100) * UMFANG

    return (
        <div className="progress-ring" data-ton={ton}>
            <svg viewBox="0 0 100 100" role="img" aria-label={`${label}: ${wert} Prozent`}>
                {/* Hintergrund-Ring */}
                <circle className="progress-ring__bg" cx="50" cy="50" r={RADIUS} />
                {/* Fortschritts-Ring: Start oben (-90 Grad Drehung) */}
                <circle
                    className="progress-ring__fg"
                    cx="50"
                    cy="50"
                    r={RADIUS}
                    strokeDasharray={`${gefuellt} ${UMFANG}`}
                    transform="rotate(-90 50 50)"
                />
            </svg>
            <div className="progress-ring__mitte">
                <span className="progress-ring__wert">{wert}%</span>
            </div>
            <span className="progress-ring__label" title={label}>{label}</span>
        </div>
    )
}
