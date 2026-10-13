import { NavLink } from 'react-router-dom';
import { useSession } from '../useSession.js';
import { clearSession } from '../session.js';
export default function Navbar() {
    const session = useSession();
    return <header className="header"><div className="header-inner">
        <div className="brand"><div className="logo">F</div><div>Fin<span>Try</span></div></div>
        <nav className="nav">
            <NavLink to="/">Anasayfa</NavLink><NavLink to="/markets">Piyasalar</NavLink>
            <NavLink to="/portfolio">Portföyüm</NavLink><NavLink to="/trade">Alım-Satım</NavLink>
            <NavLink to="/news">Haberler</NavLink><NavLink to="/help">Yardım</NavLink>
            {session?.user.role === 'ADMIN' && <NavLink to="/admin">Yönetim</NavLink>}
        </nav>
        <div className="auth">{session ? <><span>{session.user.username}</span>
            <button className="btn" onClick={clearSession}>Çıkış yap</button></> : <>
            <NavLink className="btn" to="/login">Giriş yap</NavLink>
            <NavLink className="btn primary" to="/register">Kayıt ol</NavLink></>}
        </div>
    </div></header>;
}
