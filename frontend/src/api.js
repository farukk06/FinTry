import { clearSession, getSession } from './session.js';

const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');
export class ApiError extends Error {
    constructor(status, message) { super(message); this.status = status; }
}
export async function api(path, { method = 'GET', body, signal, authenticated = true } = {}) {
    const session = getSession();
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (authenticated && session) headers.Authorization = `Bearer ${session.accessToken}`;
    const response = await fetch(`${API_URL}${path}`, {
        method, headers, signal, credentials: 'omit',
        ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    const data = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        if (authenticated && response.status === 401 && getSession() === session) clearSession();
        throw new ApiError(response.status, data?.message || 'İstek tamamlanamadı.');
    }
    return data;
}
