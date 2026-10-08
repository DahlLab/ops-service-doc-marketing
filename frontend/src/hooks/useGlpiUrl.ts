import { useEffect, useState } from 'react';
import { api } from '../api/api';

interface GlpiUrlResponse {
    url: string;
}

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

    return loggedIn ? url : '';
}
