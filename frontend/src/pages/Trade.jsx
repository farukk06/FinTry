import { useState } from 'react';
import { useApi } from '../useApi.js';
import { api } from '../api.js';
export default function Trade() {
    const { data: instruments, error, loading } = useApi('/instruments');
    const [instrumentId, setInstrumentId] = useState('');
    const [quantity, setQuantity] = useState('');
    const [pending, setPending] = useState(false);
    const [notice, setNotice] = useState('');
    async function trade(type) {
        if (pending) return;
        if (!instrumentId || !quantity || Number(quantity) <= 0) { setNotice('Enstrüman ve pozitif miktar seçin.'); return; }
        setPending(true); setNotice('');
        try {
            await api(`/transactions/${type}`, { method: 'POST', body: { instrumentId: Number(instrumentId), quantity } });
            setNotice(type === 'buy' ? 'Alım tamamlandı.' : 'Satış tamamlandı.'); setQuantity('');
        } catch (failure) { setNotice(failure.message); }
        finally { setPending(false); }
    }
    return <><h1>Alım-Satım</h1><section className="card trade-form">
        {loading && <p>Yükleniyor…</p>}{error && <p role="alert">{error}</p>}
        <label>Enstrüman<select value={instrumentId} onChange={e => setInstrumentId(e.target.value)} disabled={pending}>
            <option value="">Enstrüman seçin</option>{instruments?.map(i => <option key={i.id} value={i.id}>{i.symbol} — {i.name}</option>)}
        </select></label>
        <label>Miktar<input type="number" min="0.00000001" step="any" value={quantity} onChange={e => setQuantity(e.target.value)} disabled={pending} /></label>
        <button className="btn" disabled={pending || loading || !!error} onClick={() => trade('buy')}>Al</button>
        <button className="btn" disabled={pending || loading || !!error} onClick={() => trade('sell')}>Sat</button>
        {notice && <p role="status">{notice}</p>}
    </section></>;
}
