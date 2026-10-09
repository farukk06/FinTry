import { Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Markets from './pages/Markets.jsx';
import Portfolio from './pages/Portfolio.jsx';
import Trade from './pages/Trade.jsx';
import News from './pages/News.jsx';
import Analysis from './pages/Analysis.jsx';
import Help from './pages/Help.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Admin from './pages/Admin.jsx';
export default function AppRoutes() {
    return <Routes>
        <Route path="/" element={<Dashboard />} /><Route path="/news" element={<News />} />
        <Route path="/analysis" element={<Analysis />} /><Route path="/help" element={<Help />} />
        <Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} />
        <Route element={<ProtectedRoute />}>
            <Route path="/markets" element={<Markets />} /><Route path="/portfolio" element={<Portfolio />} />
            <Route path="/trade" element={<Trade />} />
        </Route>
        <Route element={<ProtectedRoute role="ADMIN" />}><Route path="/admin" element={<Admin />} /></Route>
        <Route path="*" element={<p>Sayfa bulunamadı.</p>} />
    </Routes>;
}
