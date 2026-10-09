import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useSession } from '../useSession.js';
export default function ProtectedRoute({ role }) {
    const session = useSession();
    const location = useLocation();
    if (!session) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
    if (role && session.user.role !== role) return <p role="alert">Bu sayfa için yetkiniz yok.</p>;
    return <Outlet key={`${session.user.id}:${session.expiresAt}`} />;
}
