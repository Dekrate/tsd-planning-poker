import {BrowserRouter as Router, Route, Routes} from 'react-router-dom';
import {HomePage} from './pages/HomePage';

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/join-session" element={<HomePage/>}/>
                <Route path="*" element={<div>Page Not Found</div>}/>
            </Routes>
        </Router>
    );
}

export default App;