import { useApi } from '../useApi.js';
export default function Markets() {
    const { data: instruments, error, loading } = useApi('/instruments');
    return <><h1>Piyasalar</h1>{loading && <p>Yükleniyor…</p>}{error && <p role="alert">{error}</p>}
        <section className="card"><table><thead><tr><th>Sembol</th><th>Ad</th><th>Tür</th><th>Fiyat</th></tr></thead>
            <tbody>{instruments?.map(item => <tr key={item.id}><td>{item.symbol}</td><td>{item.name}</td>
                <td>{item.type}</td><td>{item.price} ₺</td></tr>)}</tbody>
        </table></section></>;
}
