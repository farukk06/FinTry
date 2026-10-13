import { useState } from 'react';
import { useApi } from '../useApi.js';
import { api } from '../api.js';
function PendingRequests() {
    const { data, error, loading } = useApi('/admin/balance-requests');
    const [approved, setApproved] = useState([]);
    const [pending, setPending] = useState(false);
    const [notice, setNotice] = useState('');
    async function approve(id) {
        if (pending) return; setPending(true); setNotice('');
        try { await api(`/balance-requests/${id}/approve`, { method: 'PUT' }); setApproved(previous => [...previous, id]); }
        catch (failure) { setNotice(failure.message); }
        finally { setPending(false); }
    }
    return <section className="card"><h2>Bekleyen bakiye talepleri</h2>
        {loading && <p>Yükleniyor…</p>}{error && <p role="alert">{error}</p>}{notice && <p role="alert">{notice}</p>}
        <ul>{data?.filter(r => !approved.includes(r.id)).map(r => <li key={r.id}>Talep {r.id} · Kullanıcı {r.userId} · {r.requestedAmount} ₺
            <button className="btn" disabled={pending} onClick={() => approve(r.id)}>Onayla</button></li>)}</ul>
    </section>;
}
function PriceUpdate() {
    const [notice, setNotice] = useState('');
    const [pending, setPending] = useState(false);
    async function update(event) {
        event.preventDefault(); if (pending) return;
        const form = new FormData(event.currentTarget); setPending(true); setNotice('');
        try { await api(`/instruments/${encodeURIComponent(form.get('id'))}/price?price=${encodeURIComponent(form.get('price'))}`, { method: 'PUT' }); setNotice('Fiyat güncellendi.'); }
        catch (failure) { setNotice(failure.message); }
        finally { setPending(false); }
    }
    return <section className="card"><h2>Fiyat güncelle</h2><form onSubmit={update}>
        <label>Enstrüman ID<input name="id" type="number" min="1" step="1" required /></label>
        <label>Fiyat<input name="price" type="number" min="0.00000001" step="any" required /></label>
        <button className="btn" disabled={pending}>Güncelle</button>
    </form>{notice && <p role="status">{notice}</p>}</section>;
}
export default function Admin() { return <><h1>Yönetim</h1><PendingRequests /><PriceUpdate /></>; }
