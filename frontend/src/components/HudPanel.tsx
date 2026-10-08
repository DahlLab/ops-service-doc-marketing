import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

interface HudPanelProps {
    title: string

    children: ReactNode

    footerLink?: { to: string; label: string }

    tone?: 'default' | 'warning' | 'danger'

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
        <section className={`hud-panel ${className}`.trim()} data-tone={tone}>

            <div className="hud-panel__inner">

                <h2 className="hud-panel__title">{title}</h2>

                <div className="hud-panel__body">{children}</div>

                {footerLink && (
                    <div className="hud-panel__footer">
                        <Link to={footerLink.to}>{footerLink.label} ›</Link>
                    </div>
                )}
            </div>
        </section>
    )
}
