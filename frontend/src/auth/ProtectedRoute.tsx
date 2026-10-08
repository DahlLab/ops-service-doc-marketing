import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import { useAuth } from './useAuth';

// Schützt eine Route: ist niemand eingeloggt, leite ich auf die
// Startseite um, statt die geschützte Seite überhaupt zu rendern.
export function ProtectedRoute({ children }: Readonly<{ children: ReactNode }>) {
    const { username, loading } = useAuth();

    // Während die erste /api/auth/me-Abfrage noch läuft, weiß ich noch
    // nicht, ob jemand eingeloggt ist - in dem Moment schon auf "/"
    // umzuleiten, würde bei jedem Seiten-Reload kurz zur Startseite
    // zurückspringen, auch wenn der Nutzer eigentlich eingeloggt ist.
    if (loading) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        );
    }

    if (username === null) {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
}