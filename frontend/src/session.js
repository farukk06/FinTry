let session = null;
let timer;
const listeners = new Set();
function publish() { listeners.forEach(listener => listener()); }
export function getSession() { return session; }
export function subscribe(listener) { listeners.add(listener); return () => listeners.delete(listener); }
export function clearSession() {
    clearTimeout(timer);
    session = null;
    publish();
}
export function setSession(response) {
    if (!response.accessToken || response.tokenType !== 'Bearer' || !response.user?.id ||
        !['USER', 'ADMIN'].includes(response.user.role) || !Number.isFinite(response.expiresIn) || response.expiresIn <= 0) {
        throw new Error('Geçersiz oturum yanıtı.');
    }
    clearTimeout(timer);
    session = { ...response, expiresAt: Date.now() + response.expiresIn * 1000 };
    timer = setTimeout(clearSession, response.expiresIn * 1000);
    publish();
}
