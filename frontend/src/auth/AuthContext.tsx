import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { fetchCurrentUser, holeCsrfToken } from '../api/api';
import { AuthContext, type AuthContextValue } from './useAuth';

// URL, unter der mein Backend läuft. Für den GitHub-Login und das
// Logout brauche ich eine ECHTE Browser-Weiterleitung (kein fetch),
// weil GitHub den Nutzer über mehrere Seiten hinweg umleitet - das lässt
// sich nicht per JavaScript-Request nachbilden, nur per Navigation.
// Im Dev-Betrieb läuft das Backend fest auf Port 8080.
const BACKEND_URL = 'http://localhost:8080';

export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
    const [username, setUsername] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    // Beim ersten Laden der App prüfe ich einmal, ob schon eine gültige
    // Session besteht (z.B. weil der Nutzer die Seite neu lädt, nachdem
    // er sich vorher schon eingeloggt hat).
    useEffect(() => {
        fetchCurrentUser()
            .then(setUsername)
            .catch(() => setUsername(null))
            .finally(() => setLoading(false));
    }, []);

    // Logout ist bei Spring Security standardmäßig ein POST auf /logout.
    // Ich baue mir dafür dynamisch ein unsichtbares Formular statt eines
    // fetch-Aufrufs, weil der Browser nach dem Logout per Redirect auf
    // logoutSuccessUrl (siehe SecurityConfig) weitergeleitet wird - das
    // funktioniert nur mit einer echten Formular-Navigation, nicht mit fetch.
    const logout = useCallback(() => {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `${BACKEND_URL}/logout`;
        // Spring Security verlangt auch beim Logout (POST) das CSRF-Token -
        // bei einem Formular als verstecktes Feld "_csrf" statt als Header.
        const csrfToken = holeCsrfToken();
        if (csrfToken) {
            const feld = document.createElement('input');
            feld.type = 'hidden';
            feld.name = '_csrf';
            feld.value = csrfToken;
            form.appendChild(feld);
        }
        document.body.appendChild(form);
        form.submit();
    }, []);

    // useMemo: das value-Objekt wird nur neu erzeugt, wenn sich username/loading/logout
    // wirklich ändern. Sonst bekäme JEDE Komponente, die useAuth() nutzt, bei jedem
    // Rendern ein "neues" Objekt und würde unnötig neu rendern.
    const value = useMemo<AuthContextValue>(
        () => ({
            username,
            loading,
            loginUrl: `${BACKEND_URL}/oauth2/authorization/github`,
            logout,
        }),
        [username, loading, logout],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
