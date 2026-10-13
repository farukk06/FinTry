import { BrowserRouter } from 'react-router-dom';
import './App.css';
import Navbar from './components/Navbar.jsx';
import AppRoutes from './AppRoutes.jsx';
export default function App() {
    return <BrowserRouter><Navbar /><main className="container"><AppRoutes /></main></BrowserRouter>;
}
