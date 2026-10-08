export class ApiError extends Error {
    status: number;

    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

function readCookie(name: string): string | null {
    for (const entry of document.cookie.split(';')) {
        const separator = entry.indexOf('=');
        if (separator > 0 && entry.slice(0, separator).trim() === name) {
            return decodeURIComponent(entry.slice(separator + 1).trim());
        }
    }
    return null;
}

export function fetchCsrfToken(): string | null {
    return readCookie('XSRF-TOKEN');
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const method = (options.method ?? 'GET').toUpperCase();
    const csrfToken = method === 'GET' ? null : fetchCsrfToken();

    const response = await fetch(path, {
        credentials: 'include',
        ...options,

        headers: {
            'Content-Type': 'application/json',
            ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
            ...options.headers,
        },
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => '');
        throw new ApiError(response.status, errorText || response.statusText);
    }

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
