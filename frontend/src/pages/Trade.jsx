import { useEffect, useState } from "react";

function Trade() {
    const [instruments, setInstruments] = useState([]);
    const [selectedInstrumentId, setSelectedInstrumentId] = useState("");
    const [quantity, setQuantity] = useState("");

    const loadData = () => {
        fetch("http://localhost:8080/instruments")
            .then((response) => response.json())
            .then((data) => setInstruments(data))
            .catch((error) => console.error(error));
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleTrade = (type) => {
        if (!selectedInstrumentId || !quantity) {
            alert("Lütfen enstrüman ve miktar seç.");
            return;
        }

        fetch(`http://localhost:8080/transactions/${type}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                userId: 1,
                instrumentId: Number(selectedInstrumentId),
                quantity: Number(quantity),
            }),
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("İşlem başarısız oldu.");
                }
                return response.json();
            })
            .then(() => {
                alert(
                    type === "buy"
                        ? "Alım işlemi başarılı."
                        : "Satım işlemi başarılı."
                );
                setQuantity("");
            })
            .catch((error) => {
                alert(error.message);
            });
    };

    return (
        <>
            <h1>Buy / Sell</h1>

            <section className="card trade-card">
                <div className="trade-form">
                    <select
                        value={selectedInstrumentId}
                        onChange={(e) => setSelectedInstrumentId(e.target.value)}
                    >
                        <option value="">Select Instrument</option>

                        {instruments.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.symbol} - {item.name}
                            </option>
                        ))}
                    </select>

                    <input
                        type="number"
                        placeholder="Quantity"
                        value={quantity}
                        onChange={(e) => setQuantity(e.target.value)}
                    />

                    <button
                        className="buy-btn"
                        onClick={() => handleTrade("buy")}
                    >
                        Buy
                    </button>

                    <button
                        className="sell-btn"
                        onClick={() => handleTrade("sell")}
                    >
                        Sell
                    </button>
                </div>
            </section>
        </>
    );
}

export default Trade;