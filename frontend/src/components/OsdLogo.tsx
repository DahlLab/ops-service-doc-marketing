/**
 * OsdLogo - das Logo von OpsServiceDoc: goldenes Sechseck mit "OSD",
 * daneben der Name und die kleine Unterzeile.
 *
 * Ich zeichne das Sechseck als SVG, weil es dadurch scharf in jeder Größe
 * bleibt und keine Bilddatei geladen werden muss. `compact` blendet den
 * Text aus (z. B. für sehr schmale Header).
 */
interface OsdLogoProps {
    /** true = nur das Sechseck ohne Schriftzug */
    compact?: boolean
}

// Readonly<...> markiert die Props als schreibgeschützt - ich darf sie in der Komponente nicht ändern
export function OsdLogo({ compact = false }: Readonly<OsdLogoProps>) {
    return (
        <div className="osd-logo">
            {/* viewBox legt das interne Koordinatensystem fest (100 x 100),
                width/height per CSS skalieren es dann. */}
            <svg className="osd-logo__hex" viewBox="0 0 100 100" aria-hidden="true">
                {/* Sechseck: sechs Eckpunkte im Uhrzeigersinn */}
                <polygon
                    points="50,4 90,27 90,73 50,96 10,73 10,27"
                    fill="rgba(227,182,87,0.08)"
                    stroke="#e3b657"
                    strokeWidth="3"
                />
                {/* feiner innerer Rahmen für den HUD-Look */}
                <polygon
                    points="50,13 82,31 82,69 50,87 18,69 18,31"
                    fill="none"
                    stroke="#e3b657"
                    strokeOpacity="0.35"
                    strokeWidth="1"
                />
                <text
                    x="50"
                    y="58"
                    textAnchor="middle"
                    fontSize="26"
                    fontWeight="600"
                    fill="#e3b657"
                    fontFamily="Segoe UI, system-ui, sans-serif"
                    letterSpacing="1"
                >
                    OSD
                </text>
            </svg>

            {!compact && (
                <div className="osd-logo__text">
                    <span className="osd-logo__name">OpsServiceDoc</span>
                    <span className="osd-logo__sub">IT Service Documentation</span>
                </div>
            )}
        </div>
    )
}
