import { createContext, useContext } from 'react';

export interface AuthContextValue {
    username: string | null;
    loading: boolean;
    loginUrl: string;
    logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth muss innerhalb eines AuthProvider verwendet werden');
    }
    return context;
}
