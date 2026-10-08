import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { fetchCurrentUser, fetchCsrfToken } from '../api/api';
import { AuthContext, type AuthContextValue } from './useAuth';

const BACKEND_URL = 'http://localhost:8080';

export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
    const [username, setUsername] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchCurrentUser()
            .then(setUsername)
            .catch(() => setUsername(null))
            .finally(() => setLoading(false));
    }, []);

    const logout = useCallback(() => {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `${BACKEND_URL}/logout`;

        const csrfToken = fetchCsrfToken();
        if (csrfToken) {
            const field = document.createElement('input');
            field.type = 'hidden';
            field.name = '_csrf';
            field.value = csrfToken;
            form.appendChild(field);
        }
        document.body.appendChild(form);
        form.submit();
    }, []);

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
