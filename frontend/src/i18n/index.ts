import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

type Bundle = Record<string, Record<string, unknown>>

const modules = import.meta.glob<{ default: Record<string, unknown> }>('../locales/*/*.json', { eager: true })

const resources: Record<string, Bundle> = {}
const namespaces = new Set<string>()
for (const [path, module] of Object.entries(modules)) {
    const match = /locales\/([^/]+)\/([^/]+)\.json$/.exec(path)
    if (!match) continue
    const [, language, namespace] = match
    resources[language] ??= {}
    resources[language][namespace] = module.default
    namespaces.add(namespace)
}

export const SUPPORTED_LANGUAGES = ['de', 'en'] as const
export type Language = (typeof SUPPORTED_LANGUAGES)[number]

function initialLanguage(): Language {
    try {
        const stored = localStorage.getItem('language')
        if (stored === 'de' || stored === 'en') return stored
    } catch {
        // storage unavailable
    }
    return navigator.language.toLowerCase().startsWith('de') ? 'de' : 'en'
}

void i18n.use(initReactI18next).init({
    resources,
    lng: initialLanguage(),
    fallbackLng: 'en',
    ns: [...namespaces],
    defaultNS: 'common',
    interpolation: { escapeValue: false },
})

i18n.on('languageChanged', (language) => {
    document.documentElement.lang = language
    try {
        localStorage.setItem('language', language)
    } catch {
        // storage unavailable
    }
})
document.documentElement.lang = i18n.language

export function currentLocale(): string {
    return i18n.language === 'de' ? 'de-DE' : 'en-GB'
}

export default i18n
