import { it, expect, vi } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AppRoutes from '../AppRoutes.jsx';
import Navbar from '../components/Navbar.jsx';
import { setSession, getSession, clearSession } from '../session.js';
const login = { accessToken: 'token', tokenType: 'Bearer', expiresIn: 900, user: { id: 1, username: 'alice', role: 'USER' } };
const response = (status, data) => ({ ok: status < 400, status, json: async () => data });
function open(path) { render(<MemoryRouter initialEntries={[path]}><Navbar /><AppRoutes /></MemoryRouter>); }
function financialFetch(url) {
    if (url.endsWith('/accounts/me')) return Promise.resolve(response(200, { balance: 0 }));
    return Promise.resolve(response(200, []));
}
it('redirects an anonymous protected page to login without requesting private data', () => {
    const fetch = vi.spyOn(globalThis, 'fetch'); open('/portfolio');
    expect(screen.getByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument(); expect(fetch).not.toHaveBeenCalled();
});
it('logs in and loads personal endpoints with no user ID', async () => {
    const user = userEvent.setup();
    const fetch = vi.spyOn(globalThis, 'fetch').mockImplementation(url => url.endsWith('/auth/login') ? Promise.resolve(response(200, login)) : financialFetch(url));
    open('/portfolio');
    await user.type(screen.getByLabelText('E-posta'), 'alice@test.example');
    await user.type(screen.getByLabelText('Parola'), 'test-password-123');
    await user.click(screen.getByRole('button', { name: 'Giriş yap' }));
    expect(await screen.findByRole('heading', { name: 'Portföyüm' })).toBeInTheDocument();
    await screen.findByText('Portföyünüz boş.');
    expect(fetch.mock.calls.map(call => call[0])).toEqual(expect.arrayContaining([
        expect.stringContaining('/accounts/me'), expect.stringContaining('/portfolio/me'), expect.stringContaining('/transactions/me'),
    ]));
    expect(fetch.mock.calls.some(call => call[0].includes('/user/1'))).toBe(false);
    await user.click(screen.getByRole('button', { name: 'Çıkış yap' }));
    expect(await screen.findByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'İşlem geçmişim' })).not.toBeInTheDocument(); expect(getSession()).toBeNull();
});
it('registers without a role or balance and directs the user to login', async () => {
    const user = userEvent.setup(); const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(201, { id: 4, role: 'USER' }));
    open('/register');
    await user.type(screen.getByLabelText('Kullanıcı adı'), 'newuser'); await user.type(screen.getByLabelText('E-posta'), 'new@test.example');
    await user.type(screen.getByLabelText('Parola', { exact: true }), 'test-password-123');
    await user.type(screen.getByLabelText('Parola tekrar'), 'test-password-123'); await user.click(screen.getByRole('button', { name: 'Kayıt ol' }));
    expect(await screen.findByText('Kayıt tamamlandı. Hesabınıza giriş yapabilirsiniz.')).toBeInTheDocument();
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ username: 'newuser', email: 'new@test.example', password: 'test-password-123' });
    expect(getSession()).toBeNull();
});
it('blocks user access to the admin page before requesting management data', () => {
    setSession(login); const fetch = vi.spyOn(globalThis, 'fetch'); open('/admin');
    expect(screen.getByRole('alert')).toHaveTextContent('Bu sayfa için yetkiniz yok.'); expect(fetch).not.toHaveBeenCalled();
});
it('renders management for ADMIN', async () => {
    setSession({ ...login, user: { ...login.user, role: 'ADMIN' } }); vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(200, []));
    open('/admin'); expect(await screen.findByRole('heading', { name: 'Yönetim' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Yönetim' })).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText('Yükleniyor…')).not.toBeInTheDocument());
});
it('removes private content when session expires', async () => {
    setSession(login); vi.spyOn(globalThis, 'fetch').mockImplementation(financialFetch); open('/portfolio');
    await screen.findByText('Portföyünüz boş.'); act(() => clearSession());
    expect(screen.getByRole('heading', { name: 'Giriş yap' })).toBeInTheDocument(); expect(screen.queryByText('Portföyünüz boş.')).not.toBeInTheDocument();
});
it('sends trades without userId and retains quantity as decimal text', async () => {
    setSession(login); const user = userEvent.setup();
    const fetch = vi.spyOn(globalThis, 'fetch').mockImplementation(url => Promise.resolve(response(200, url.endsWith('/instruments') ? [{ id: 7, symbol: 'TEST', name: 'Test' }] : {})));
    open('/trade'); await screen.findByRole('option', { name: 'TEST — Test' });
    await user.selectOptions(screen.getByLabelText('Enstrüman'), '7'); await user.type(screen.getByLabelText('Miktar'), '0.12345678');
    await user.click(screen.getByRole('button', { name: 'Al' })); await screen.findByText('Alım tamamlandı.');
    const trade = fetch.mock.calls.find(call => call[0].endsWith('/transactions/buy'));
    expect(JSON.parse(trade[1].body)).toEqual({ instrumentId: 7, quantity: '0.12345678' });
});
