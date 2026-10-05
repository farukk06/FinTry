import { useEffect, useState } from "react";

function Portfolio() {
    const [portfolio, setPortfolio] = useState([]);

    const formatCurrency = (value) => {
        return (
            Number(value).toLocaleString("tr-TR", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
            }) + " ₺"
        );
    };

    const totalValue = portfolio.reduce(
        (sum, item) => sum + item.totalValue,
        0
    );

    const totalProfitLoss = portfolio.reduce(
        (sum, item) => sum + item.profitLoss,
        0
    );

    useEffect(() => {
        fetch("http://localhost:8080/portfolio/user/1")
            .then((response) => response.json())
            .then((data) => setPortfolio(data))
            .catch((error) => console.error(error));
    }, []);

    return (
        <>
            <div className="page-head">
                <div>
                    <h1>Portföyüm</h1>
                    <p>Kullanıcının sanal yatırım varlıkları ve kâr/zarar özeti</p>
                </div>
                <span>Para birimi: TRY</span>
            </div>

            <div className="portfolio-kpis">
                <div className="portfolio-kpi">
                    <h3>Portföy Değeri</h3>
                    <p>{formatCurrency(totalValue)}</p>
                </div>

                <div className="portfolio-kpi">
                    <h3>Toplam Kâr / Zarar</h3>
                    <p className={totalProfitLoss >= 0 ? "profit" : "loss"}>
                        {formatCurrency(totalProfitLoss)}
                    </p>
                </div>

                <div className="portfolio-kpi">
                    <h3>Günlük K/Z</h3>
                    <p className="loss">-420,00 ₺</p>
                </div>

                <div className="portfolio-kpi">
                    <h3>Nakit (Sanal)</h3>
                    <p>7.495,00 ₺</p>
                </div>
            </div>

            <section className="card">
                <div className="portfolio-actions">
                    <select>
                        <option>Tüm Varlıklar</option>
                        <option>Hisse</option>
                        <option>Döviz</option>
                        <option>Altın</option>
                        <option>Kripto</option>
                    </select>

                    <button>Yeni İşlem</button>
                    <button>İşlem Geçmişi</button>
                </div>

                {portfolio.length === 0 ? (
                    <p>Portföy boş veya veri yükleniyor...</p>
                ) : (
                    <table>
                        <thead>
                        <tr>
                            <th>Varlık</th>
                            <th>Adet</th>
                            <th>Ortalama Maliyet</th>
                            <th>Güncel Fiyat</th>
                            <th>Değer</th>
                            <th>K/Z</th>
                        </tr>
                        </thead>

                        <tbody>
                        {portfolio.map((asset, index) => (
                            <tr key={index}>
                                <td>
                                    <strong>{asset.symbol}</strong>
                                    <br />
                                    <span className="muted">{asset.name}</span>
                                </td>
                                <td>{asset.quantity}</td>
                                <td>{formatCurrency(asset.averagePrice)}</td>
                                <td>{formatCurrency(asset.currentPrice)}</td>
                                <td>{formatCurrency(asset.totalValue)}</td>
                                <td className={asset.profitLoss >= 0 ? "profit" : "loss"}>
                                    {formatCurrency(asset.profitLoss)}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </section>
        </>
    );
}

export default Portfolio;