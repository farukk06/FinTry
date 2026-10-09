import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useSession } from '../useSession.js';
export default function Register() {
    const [error, setError] = useState('');
    const [pending, setPending] = useState(false);
    const navigate = useNavigate();
    const session = useSession();
    if (session) return <Navigate to="/portfolio" replace />;
    async function submit(event) {
        event.preventDefault();
        if (pending) return;
        setPending(true); setError('');
        const form = new FormData(event.currentTarget);
        try {
            if (form.get('password') !== form.get('confirmation')) throw new Error('Parolalar eşleşmiyor.');
            await api('/auth/register', { method: 'POST', authenticated: false, body: {
                username: form.get('username'), email: form.get('email'), password: form.get('password'),
            } });
            navigate('/login', { replace: true, state: { registered: true } });
        } catch (failure) { setError(failure.message); }
        finally { setPending(false); }
    }
    return <section className="card auth-card"><h1>Kayıt ol</h1><form onSubmit={submit}>
        <label>Kullanıcı adı<input name="username" autoComplete="username" required minLength={3} maxLength={64} pattern="[A-Za-z0-9_.-]+" /></label>
        <label>E-posta<input name="email" type="email" autoComplete="email" required maxLength={254} /></label>
        <label>Parola<input name="password" type="password" autoComplete="new-password" required minLength={12} maxLength={72} /></label>
        <label>Parola tekrar<input name="confirmation" type="password" autoComplete="new-password" required minLength={12} maxLength={72} /></label>
        <p>En az 12 karakter kullanın. Sanal hesabınız sıfır bakiye ile oluşturulur.</p>
        {error && <p role="alert">{error}</p>}
        <button className="btn primary" disabled={pending}>{pending ? 'Kayıt yapılıyor…' : 'Kayıt ol'}</button>
    </form><p>Zaten kayıtlı mısınız? <Link to="/login">Giriş yapın</Link></p></section>;
}
