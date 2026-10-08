import { useTranslation } from 'react-i18next'

interface OsdLogoProps {
    compact?: boolean
}

export function OsdLogo({ compact = false }: Readonly<OsdLogoProps>) {
    const { t } = useTranslation('home')

    return (
        <div className="osd-logo">

            <svg className="osd-logo__hex" viewBox="0 0 100 100" aria-hidden="true">

                <polygon
                    points="50,4 90,27 90,73 50,96 10,73 10,27"
                    fill="rgba(227,182,87,0.08)"
                    stroke="#e3b657"
                    strokeWidth="3"
                />

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
                    <span className="osd-logo__sub">{t('logo.tagline')}</span>
                </div>
            )}
        </div>
    )
}
