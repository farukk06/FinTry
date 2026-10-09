import { describe, it, expect, vi } from 'vitest';
import { api, ApiError } from '../api.js';
import { setSession, getSession, clearSession } from '../session.js';
const login = { accessToken: 'token', tokenType: 'Bearer', expiresIn: 900, user: { id: 1, username: 'alice', role: 'USER' } };
const response = (status, data) => ({ ok: status < 400, status, json: async () => data });
describe('session and API contract', () => {
    it('adds Bearer explicitly without cookies or browser storage', async () => {
        setSession(login);
        const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(200, []));
        await api('/transactions/buy', { method: 'POST', body: { instrumentId: 7, quantity: '0.12345678' } });
        expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/transactions/buy'), expect.objectContaining({
            credentials: 'omit', headers: { Authorization: 'Bearer token', 'Content-Type': 'application/json' },
            body: '{"instrumentId":7,"quantity":"0.12345678"}',
        }));
        expect(localStorage.length).toBe(0); expect(sessionStorage.length).toBe(0);
    });
    it('clears session on 401 and preserves it on 403 without retry', async () => {
        setSession(login);
        const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(403, { message: 'Access denied' }));
        await expect(api('/users')).rejects.toMatchObject({ status: 403 });
        expect(getSession()).not.toBeNull();
        fetch.mockResolvedValue(response(401, { message: 'Authentication required' }));
        await expect(api('/accounts/me')).rejects.toBeInstanceOf(ApiError);
        expect(getSession()).toBeNull(); expect(fetch).toHaveBeenCalledTimes(2);
    });
    it('expires access token and clears it on logout', () => {
        vi.useFakeTimers(); setSession({ ...login, expiresIn: 1 });
        vi.advanceTimersByTime(1000); expect(getSession()).toBeNull();
        setSession(login); clearSession(); expect(getSession()).toBeNull();
    });
    it('ignores a late 401 from an earlier session', async () => {
        setSession(login);
        let finish;
        vi.spyOn(globalThis, 'fetch').mockImplementation(() => new Promise(resolve => { finish = resolve; }));
        const request = api('/accounts/me');
        setSession({ ...login, accessToken: 'new-token' });
        finish(response(401, { message: 'Expired' }));
        await expect(request).rejects.toMatchObject({ status: 401 });
        expect(getSession().accessToken).toBe('new-token');
    });
    it('never attaches an existing token to registration or login', async () => {
        setSession(login);
        const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(401, { message: 'Invalid email or password' }));
        await expect(api('/auth/login', { authenticated: false, method: 'POST', body: { email: 'a@b.test', password: 'wrong' } })).rejects.toMatchObject({ status: 401 });
        expect(fetch.mock.calls[0][1].headers.Authorization).toBeUndefined(); expect(getSession()).not.toBeNull();
    });
});
