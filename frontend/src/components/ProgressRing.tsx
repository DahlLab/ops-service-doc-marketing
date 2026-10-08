import { useTranslation } from 'react-i18next'

interface ProgressRingProps {
    percent: number

    label: string

    tone?: 'cyan' | 'gold' | 'red'
}

const RADIUS = 42

const CIRCUMFERENCE = 2 * Math.PI * RADIUS

export function ProgressRing({ percent, label, tone = 'cyan' }: Readonly<ProgressRingProps>) {
    const { t } = useTranslation('home')

    const value = Math.max(0, Math.min(100, Math.round(percent)))

    const filled = (value / 100) * CIRCUMFERENCE

    return (
        <div className="progress-ring" data-tone={tone}>
            <svg viewBox="0 0 100 100" role="img" aria-label={t('progressRing.ariaLabel', { label, value })}>

                <circle className="progress-ring__bg" cx="50" cy="50" r={RADIUS} />

                <circle
                    className="progress-ring__fg"
                    cx="50"
                    cy="50"
                    r={RADIUS}
                    strokeDasharray={`${filled} ${CIRCUMFERENCE}`}
                    transform="rotate(-90 50 50)"
                />
            </svg>
            <div className="progress-ring__center">
                <span className="progress-ring__value">{value}%</span>
            </div>
            <span className="progress-ring__label" title={label}>{label}</span>
        </div>
    )
}
