import {BrowserRouter as Router, Route, Routes} from 'react-router-dom';
import {HomePage} from './pages/HomePage';

function App() {
    return (
        // Dodaj prop basename, który musi być taki sam jak 'base' w vite.config.ts
        <Router basename="/tsd-planning-poker/">
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/join-session" element={<HomePage/>}/>
                <Route path="*" element={<div>Page Not Found</div>}/>
            </Routes>
        </Router>
    );
}

export default App;
