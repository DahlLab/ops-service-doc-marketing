import { useEffect, useState } from 'react';
import { api } from '../api/api';

interface GlpiUrlResponse {
    url: string;
}

// Holt die Adresse der GLPI-Weboberfläche vom Backend (GET
// /api/config/glpi-url), solange jemand eingeloggt ist. Liefert einen
// leeren String, solange nichts geladen ist oder keine URL konfiguriert
// ist - die aufrufende Komponente blendet den Link dann aus.
export function useGlpiUrl(loggedIn: boolean): string {
    const [url, setUrl] = useState('');

    useEffect(() => {
        if (!loggedIn) {
            return;
        }
        api.get<GlpiUrlResponse>('/api/config/glpi-url')
            .then((reply) => setUrl(reply.url))
            .catch(() => setUrl(''));
    }, [loggedIn]);

    // Ausgeloggt gebe ich immer '' zurück, ohne dafür synchron setState im
    // Effect aufzurufen (react-hooks/set-state-in-effect): die URL wird nur
    // im Promise-Callback gesetzt und hier beim Rückgeben ausgeblendet.
    return loggedIn ? url : '';
}
