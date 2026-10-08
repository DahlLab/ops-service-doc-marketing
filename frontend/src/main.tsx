import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import './i18n'
import './index.css'

import 'bootstrap/dist/css/bootstrap.min.css'

import './theme.css'

import './hud-components.css'

import { AuthProvider } from './auth/AuthContext'
import App from './App.tsx'

document.addEventListener('click', (event) => {
    const button = (event.target as HTMLElement).closest<HTMLElement>('.btn')
    if (!button) return

    button.classList.remove('hud-pulse')

    button.getBoundingClientRect()
    button.classList.add('hud-pulse')
    button.addEventListener('animationend', () => button.classList.remove('hud-pulse'), { once: true })
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
