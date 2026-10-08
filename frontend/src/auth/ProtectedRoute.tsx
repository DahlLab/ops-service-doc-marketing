import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import { useAuth } from './useAuth';

export function ProtectedRoute({ children }: Readonly<{ children: ReactNode }>) {
    const { username, loading } = useAuth();

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
