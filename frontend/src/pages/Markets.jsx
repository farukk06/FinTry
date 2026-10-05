import { useEffect, useState } from "react";

function Markets() {
    const [instruments, setInstruments] = useState([]);

    const formatCurrency = (value) => {
        return Number(value).toLocaleString("tr-TR", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        }) + " ₺";
    };

    useEffect(() => {
        fetch("http://localhost:8080/instruments")
            .then((response) => response.json())
            .then((data) => setInstruments(data))
            .catch((error) => console.error(error));
    }, []);

    return (
        <>
            <div className="page-head">
                <div>
                    <h1>Piyasalar</h1>
                    <p>Hisse, döviz, altın ve kripto enstrümanları</p>
                </div>
                <span>Veriler örnek amaçlıdır.</span>
            </div>

            <section className="card">
                <div className="market-filters">
                    <input type="text" placeholder="Örn: THYAO, USDTRY, Altın..." />

                    <select>
                        <option>Tümü</option>
                        <option>STOCK</option>
                        <option>FOREX</option>
                        <option>GOLD</option>
                        <option>CRYPTO</option>
                    </select>
                </div>

                <table>
                    <thead>
                    <tr>
                        <th>Symbol</th>
                        <th>Name</th>
                        <th>Type</th>
                        <th>Price</th>
                        <th>İşlem</th>
                    </tr>
                    </thead>

                    <tbody>
                    {instruments.map((item) => (
                        <tr key={item.id}>
                            <td>{item.symbol}</td>
                            <td>{item.name}</td>
                            <td>
                                <span className="type-pill">{item.type}</span>
                            </td>
                            <td>{formatCurrency(item.price)}</td>
                            <td>
                                <button className="detail-btn">Detay</button>
                                <button className="follow-btn">Takip</button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </section>
        </>
    );
}

export default Markets;