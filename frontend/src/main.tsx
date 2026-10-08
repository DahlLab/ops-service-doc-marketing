import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import './index.css'

// Bootstrap-CSS global einbinden, nach index.css aber vor App.css -
// so kann ich einzelne Bootstrap-Stile in App.css gezielt überschreiben.
import 'bootstrap/dist/css/bootstrap.min.css'

// HUD-Theme (Farben, Schriften, Bootstrap-Overrides) - muss NACH Bootstrap kommen
import './theme.css'

// Bootstrap-Bausteine im HUD-Look (Tabellen, Modals, Tabs, Badges, ...)
import './hud-components.css'

import { AuthProvider } from './auth/AuthContext'
import App from './App.tsx'

// Klick-Puls: Ich höre einmal global auf Klicks (Event-Delegation) und setze bei
// einem Button kurz die Klasse "hud-puls". Das CSS dazu steht in hud-components.css.
// So muss ich keinen einzelnen Button anfassen.
document.addEventListener('click', (event) => {
    const button = (event.target as HTMLElement).closest<HTMLElement>('.btn')
    if (!button) return
    // Klasse erst entfernen und neu setzen, damit auch schnelles Doppelklicken neu pulst
    button.classList.remove('hud-puls')
    // Das Auslesen der Größe erzwingt ein Neuberechnen des Layouts (Reflow), sonst startet die
    // Animation nicht neu. Ich nutze getBoundingClientRect() statt "void offsetWidth",
    // weil SonarCloud den void-Operator als verwirrend markiert.
    button.getBoundingClientRect()
    button.classList.add('hud-puls')
    button.addEventListener('animationend', () => button.classList.remove('hud-puls'), { once: true })
})

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <BrowserRouter>
            <AuthProvider>
                <App />
            </AuthProvider>
        </BrowserRouter>
    </StrictMode>,
)