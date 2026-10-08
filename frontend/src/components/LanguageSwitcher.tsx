import { useTranslation } from 'react-i18next'
import { SUPPORTED_LANGUAGES } from '../i18n'

export function LanguageSwitcher() {
    const { i18n, t } = useTranslation()

    return (
        <div className="language-switcher" role="group" aria-label={t('language.label')}>
            {SUPPORTED_LANGUAGES.map((language) => (
                <button
                    key={language}
                    type="button"
                    className={`language-switcher__option ${i18n.resolvedLanguage === language ? 'is-active' : ''}`}
                    aria-pressed={i18n.resolvedLanguage === language}
                    onClick={() => void i18n.changeLanguage(language)}
                >
                    {language.toUpperCase()}
                </button>
            ))}
        </div>
    )
}
