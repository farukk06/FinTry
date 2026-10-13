import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { setSession } from '../session.js';
import { useSession } from '../useSession.js';
export default function Login() {
    const [error, setError] = useState('');
    const [pending, setPending] = useState(false);
    const session = useSession();
    const navigate = useNavigate();
    const location = useLocation();
    const target = location.state?.from || '/portfolio';
    if (session) return <Navigate to={target} replace />;
    async function submit(event) {
        event.preventDefault();
        if (pending) return;
        setPending(true); setError('');
        const form = new FormData(event.currentTarget);
        try {
            const response = await api('/auth/login', { method: 'POST', authenticated: false,
                body: { email: form.get('email'), password: form.get('password') } });
            setSession(response);
            navigate(target, { replace: true });
        } catch (failure) { setError(failure.message); }
        finally { setPending(false); }
    }
    return <section className="card auth-card"><h1>Giriş yap</h1>
        {location.state?.registered && <p>Kayıt tamamlandı. Hesabınıza giriş yapabilirsiniz.</p>}
        <form onSubmit={submit}>
            <label>E-posta<input name="email" type="email" autoComplete="username" required maxLength={254} /></label>
            <label>Parola<input name="password" type="password" autoComplete="current-password" required maxLength={72} /></label>
            {error && <p role="alert">{error}</p>}
            <button className="btn primary" disabled={pending}>{pending ? 'Giriş yapılıyor…' : 'Giriş yap'}</button>
        </form><p>Hesabınız yok mu? <Link to="/register">Kayıt olun</Link></p>
    </section>;
}
