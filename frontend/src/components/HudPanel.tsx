import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

/**
 * HudPanel - die zentrale Bauplan-Komponente für das HUD-Design.
 *
 * Jeder Inhaltsblock (Home-Karten, Tabellen, Formulare) wird in ein HudPanel
 * gesteckt. So sieht alles gleich aus, und ich muss das Aussehen (abgeschrägte
 * Ecken, dünne Linie, Titel) nur an dieser einen Stelle pflegen.
 */

// Die Props beschreibe ich als eigenen Typ, damit TypeScript mir Fehler zeigt,
// wenn ich das Panel falsch benutze (z. B. den Titel vergesse).
interface HudPanelProps {
    /** Titel oben im Panel - wird per CSS in Großbuchstaben mit weitem Abstand gezeigt */
    title: string
    /** Alles, was im Panel stehen soll */
    children: ReactNode
    /** Optionaler Link unten rechts, z. B. "Alle anzeigen" - führt per Router auf eine Seite */
    footerLink?: { to: string; label: string }
    /** Farbton der Linie: normal (Stahlblau), Warnung (Gold) oder kritisch (Rot) */
    tone?: 'default' | 'warning' | 'danger'
    /** Optional zusätzliche CSS-Klassen, z. B. für Grid-Platzierung von außen */
    className?: string
}

export default function HudPanel({
                                     title,
                                     children,
                                     footerLink,
                                     tone = 'default',
                                     className = '',
                                 }: Readonly<HudPanelProps>) {
    return (
        // Äußeres Element = die "Linie": Es hat die Linienfarbe als Hintergrund und ist
        // 1 px größer als das innere. Der sichtbare Rand ist also nur der Streifen dazwischen.
        // data-tone steuert über CSS die Linienfarbe.
        <section className={`hud-panel ${className}`.trim()} data-tone={tone}>
            {/* Inneres Element = die Füllung mit dem Inhalt */}
            <div className="hud-panel__inner">
                {/* h2 statt div, damit die Seitenstruktur für Screenreader stimmt */}
                <h2 className="hud-panel__title">{title}</h2>

                <div className="hud-panel__body">{children}</div>

                {/* Footer nur rendern, wenn ich einen Link übergeben habe */}
                {footerLink && (
                    <div className="hud-panel__footer">
                        <Link to={footerLink.to}>{footerLink.label} ›</Link>
                    </div>
                )}
            </div>
        </section>
    )
}
