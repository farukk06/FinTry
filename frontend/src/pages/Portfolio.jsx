import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import { api } from '../api.js';
export default function Portfolio() {
    const portfolio = useApi('/portfolio/me');
    const account = useApi('/accounts/me');
    const transactions = useApi('/transactions/me');
    const [notice, setNotice] = useState('');
    const [pending, setPending] = useState(false);
    async function requestBalance(event) {
        event.preventDefault(); if (pending) return;
        const form = event.currentTarget;
        const requestedAmount = new FormData(form).get('amount');
        setPending(true); setNotice('');
        try {
            await api('/balance-requests', { method: 'POST', body: { requestedAmount } });
            setNotice('Bakiye talebiniz yönetici onayına gönderildi.'); form.reset();
        } catch (error) { setNotice(error.message); }
        finally { setPending(false); }
    }
    return <><h1>Portföyüm</h1>
        {[portfolio, account, transactions].some(r => r.loading) && <p>Yükleniyor…</p>}
        {[portfolio, account, transactions].filter(r => r.error).map((r, index) => <p role="alert" key={index}>{r.error}</p>)}
        <section className="card"><h2>Sanal hesap</h2><p>Bakiye: {account.data?.balance ?? '—'} ₺</p>
            <form onSubmit={requestBalance}><label>Talep tutarı<input name="amount" type="number" min="0.0000000000000001" step="any" required /></label>
                <button className="btn" disabled={pending}>Bakiye talep et</button></form>
            {notice && <p role="status">{notice}</p>}
        </section>
        <section className="card"><h2>Varlıklarım</h2><Link to="/trade">Yeni işlem</Link>
            {portfolio.data?.length === 0 && <p>Portföyünüz boş.</p>}
            <table><thead><tr><th>Sembol</th><th>Miktar</th><th>Ortalama maliyet</th><th>Güncel fiyat</th><th>Değer</th><th>Kâr/Zarar</th></tr></thead>
                <tbody>{portfolio.data?.map(asset => <tr key={asset.symbol}><td>{asset.symbol}</td><td>{asset.quantity}</td>
                    <td>{asset.averagePrice}</td><td>{asset.currentPrice}</td><td>{asset.totalValue}</td><td>{asset.profitLoss}</td></tr>)}</tbody>
            </table>
        </section><section className="card"><h2>İşlem geçmişim</h2>
            {transactions.data?.length === 0 && <p>Henüz işlem yapmadınız.</p>}
            <table><thead><tr><th>Tür</th><th>Sembol</th><th>Miktar</th><th>Toplam</th><th>Tarih</th></tr></thead>
                <tbody>{transactions.data?.map(t => <tr key={t.id}><td>{t.type}</td><td>{t.instrumentSymbol}</td>
                    <td>{t.quantity}</td><td>{t.totalAmount}</td><td>{t.transactionTime}</td></tr>)}</tbody>
            </table>
        </section></>;
}
