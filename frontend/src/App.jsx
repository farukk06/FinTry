import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const [instruments, setInstruments] = useState([]);

  useEffect(() => {
    fetch("http://localhost:8080/instruments")
        .then((response) => response.json())
        .then((data) => {
          setInstruments(data);
        })
        .catch((error) => {
          console.error("Hata oluştu:", error);
        });
  }, []);

  return (
      <div className="container">
        <h1>FinTry - Market Instruments</h1>

        {instruments.length === 0 ? (
            <p>Veriler yükleniyor...</p>
        ) : (
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
        )}
      </div>
  );
}

export default App;