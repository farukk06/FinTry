import { NavLink } from "react-router-dom";

function Navbar() {
    return (
        <header className="header">
            <div className="header-inner">
                <div className="brand">
                    <div className="logo">F</div>
                    <div>
                        Fin<span>Try</span>
                    </div>
                </div>

                <nav className="nav">
                    <NavLink to="/">Anasayfa</NavLink>
                    <NavLink to="/markets">Piyasalar</NavLink>
                    <NavLink to="/news">Haberler</NavLink>
                    <NavLink to="/trade">Alım-Satım</NavLink>
                    <NavLink to="/analysis">Analiz</NavLink>
                    <NavLink to="/help">Yardım</NavLink>
                </nav>

                <div className="auth">
                    <button className="btn">Giriş Yap</button>
                    <button className="btn primary">Kayıt Ol</button>
                </div>
            </div>
        </header>
    );
}

export default Navbar;