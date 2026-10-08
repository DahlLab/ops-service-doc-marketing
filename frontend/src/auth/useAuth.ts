import { createContext, useContext } from 'react';

// Dieser Teil ist bewusst in einer eigenen Datei (statt in AuthContext.tsx):
// Dateien mit Komponenten sollen NUR Komponenten exportieren, sonst funktioniert
// das "Fast Refresh" (Live-Aktualisierung beim Entwickeln) nicht zuverlässig.
// Hier liegen deshalb der Context selbst und der Hook zum Auslesen.

export interface AuthContextValue {
    username: string | null;
    loading: boolean;
    loginUrl: string;
    logout: () => void;
}

// Startwert undefined: so merke ich, wenn jemand useAuth() außerhalb eines
// AuthProvider benutzt (siehe Prüfung im Hook unten).
export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// Eigener Hook statt useContext(AuthContext) überall, damit ich an
// zentraler Stelle prüfen kann, ob der Hook auch wirklich innerhalb
// eines AuthProvider verwendet wird.
export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth muss innerhalb eines AuthProvider verwendet werden');
    }
    return context;
}
