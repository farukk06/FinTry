import { BrowserRouter, Routes, Route } from "react-router-dom";
import "./App.css";

import Navbar from "./components/Navbar";
import Dashboard from "./pages/Dashboard";
import Markets from "./pages/Markets";
import Portfolio from "./pages/Portfolio";
import Trade from "./pages/Trade";
import News from "./pages/News";
import Analysis from "./pages/Analysis";
import Help from "./pages/Help";

function App() {
    return (
        <BrowserRouter>
            <Navbar />

            <main className="container">
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/markets" element={<Markets />} />
                    <Route path="/portfolio" element={<Portfolio />} />
                    <Route path="/trade" element={<Trade />} />
                    <Route path="/news" element={<News />} />
                    <Route path="/analysis" element={<Analysis />} />
                    <Route path="/help" element={<Help />} />
                </Routes>
            </main>
        </BrowserRouter>
    );
}

export default App;