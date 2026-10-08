// Zentrale Fetch-Hilfsfunktion für alle API-Calls ans Backend.
// Warum ich das hier bündle, statt überall "fetch(...)" einzeln
// aufzurufen:
// - "credentials: 'include'" muss ich bei JEDEM Call mitschicken, sonst
//   schickt der Browser das Session-Cookie nicht mit, und jede Anfrage
//   wäre für Spring Security "nicht eingeloggt".
// - Ich werfe bei einer Fehlerantwort (4xx/5xx) eine echte Exception mit
//   Statuscode, damit ich in den Komponenten mit try/catch sauber auf
//   Fehler reagieren kann, statt jedes Mal manuell response.ok zu prüfen.

export class ApiError extends Error {
    status: number;

    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

// Liest den Wert eines Cookies anhand seines Namens. Ich zerlege den
// Cookie-String bewusst mit split statt mit einem regulären Ausdruck -
// einfacher zu lesen und ohne Backtracking-Risiko.
function lesCookie(name: string): string | null {
    for (const eintrag of document.cookie.split(';')) {
        const trenner = eintrag.indexOf('=');
        if (trenner > 0 && eintrag.slice(0, trenner).trim() === name) {
            return decodeURIComponent(eintrag.slice(trenner + 1).trim());
        }
    }
    return null;
}

// CSRF-Schutz: Das Backend legt das Token als Cookie "XSRF-TOKEN" ab (siehe
// SecurityConfig). Bei schreibenden Aufrufen (alles außer GET) schicke ich
// es als Header "X-XSRF-TOKEN" zurück - nur meine eigene Seite kann das
// Cookie lesen, eine fremde Seite kann den Header also nicht setzen.
// Auch für das Logout-Formular in AuthContext exportiert.
export function holeCsrfToken(): string | null {
    return lesCookie('XSRF-TOKEN');
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const methode = (options.method ?? 'GET').toUpperCase();
    const csrfToken = methode === 'GET' ? null : holeCsrfToken();

    const response = await fetch(path, {
        credentials: 'include',
        ...options,
        // headers stehen NACH dem Spread von options, damit sie von
        // options.headers nicht überschrieben (und Content-Type/CSRF nicht
        // verloren) werden - dabei bleiben zusätzliche Header erhalten.
        headers: {
            'Content-Type': 'application/json',
            ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
            ...options.headers,
        },
    });

    if (!response.ok) {
        // Ich lese den Fehlertext mit, falls das Backend einen liefert (z.B.
        // vom GlobalExceptionHandler), sonst reicht der reine Statuscode.
        const errorText = await response.text().catch(() => '');
        throw new ApiError(response.status, errorText || response.statusText);
    }

    // 204 No Content (z.B. nach DELETE) hat keinen Body - response.json()
    // würde hier mit einem Parse-Fehler abbrechen, deshalb prüfe ich das
    // vorher ab.
    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}

export const api = {
    get: <T>(path: string) => request<T>(path, { method: 'GET' }),
    post: <T>(path: string, body?: unknown) =>
        request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
    put: <T>(path: string, body: unknown) =>
        request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (path: string) => request<void>(path, { method: 'DELETE' }),
};

// Spezialfall /api/auth/me: liefert reinen Text (den GitHub-Usernamen),
// kein JSON - deshalb hier als eigene Funktion statt über "api.get".
export async function fetchCurrentUser(): Promise<string | null> {
    const response = await fetch('/api/auth/me', { credentials: 'include' });
    if (response.status === 401) {
        return null;
    }
    if (!response.ok) {
        throw new ApiError(response.status, response.statusText);
    }
    return response.text();
}
