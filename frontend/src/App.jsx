import { useEffect, useState } from "react";
import "./App.css";

function App() {
    const [instruments, setInstruments] = useState([]);
    const [portfolio, setPortfolio] = useState([]);

    useEffect(() => {
        fetch("http://localhost:8080/instruments")
            .then((response) => response.json())
            .then((data) => setInstruments(data))
            .catch((error) => console.error("Instrument hatası:", error));

        fetch("http://localhost:8080/portfolio/user/1")
            .then((response) => response.json())
            .then((data) => setPortfolio(data))
            .catch((error) => console.error("Portfolio hatası:", error));
    }, []);

    return (
        <div className="container">
            <h1>FinTry Dashboard</h1>

            <section className="card">
                <h2>Market Instruments</h2>

                <table>
                    <thead>
                    <tr>
                        <th>Symbol</th>
                        <th>Name</th>
                        <th>Type</th>
                        <th>Price</th>
                    </tr>
                    </thead>

                    <tbody>
                    {instruments.map((item) => (
                        <tr key={item.id}>
                            <td>{item.symbol}</td>
                            <td>{item.name}</td>
                            <td>{item.type}</td>
                            <td>{item.price}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </section>

            <section className="card">
                <h2>User Portfolio</h2>

                {portfolio.length === 0 ? (
                    <p>Portföy boş veya veri yükleniyor...</p>
                ) : (
                    <table>
                        <thead>
                        <tr>
                            <th>Symbol</th>
                            <th>Name</th>
                            <th>Quantity</th>
                            <th>Average Price</th>
                            <th>Current Price</th>
                            <th>Total Value</th>
                            <th>Profit / Loss</th>
                        </tr>
                        </thead>

                        <tbody>
                        {portfolio.map((asset, index) => (
                            <tr key={index}>
                                <td>{asset.symbol}</td>
                                <td>{asset.name}</td>
                                <td>{asset.quantity}</td>
                                <td>{asset.averagePrice}</td>
                                <td>{asset.currentPrice}</td>
                                <td>{asset.totalValue}</td>
                                <td>{asset.profitLoss}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </section>
        </div>
    );
}

export default App;