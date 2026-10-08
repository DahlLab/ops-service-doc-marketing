import {Route, Routes} from 'react-router-dom';
import {Layout} from './components/Layout';
import {Home} from './pages/Home';
import {TicketsPage} from './pages/TicketsPage';
import {TaskPlannerPage} from './pages/TaskPlannerPage';
import {ProtectedRoute} from './auth/ProtectedRoute';
import {ChecklistenPage} from './pages/ChecklistenPage';
import {IpdGeneratorPage} from './pages/IpdGeneratorPage';
import {IpdDocumentPage} from './pages/IpdDocumentPage';
import './App.css';

// Routing-Struktur der App. Jede Seite liegt unter dem gemeinsamen
// Layout (Navigation), die noch fehlenden Unterseiten (Checklisten,
// IPD-Generator) baue ich in den nächsten Schritten und ersetze dann
// hier jeweils den Platzhalter durch die echte Komponente.
function App() {
    return (
        <Routes>
            <Route path="/" element={<Layout/>}>
                <Route index element={<Home/>}/>

                <Route
                    path="/tickets"
                    element={
                        <ProtectedRoute>
                            <TicketsPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/tasks"
                    element={
                        <ProtectedRoute>
                            <TaskPlannerPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/checklisten"
                    element={
                        <ProtectedRoute>
                            <ChecklistenPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/ipd"
                    element={
                        <ProtectedRoute>
                            <IpdGeneratorPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/ipd/:id"
                       element={
                           <ProtectedRoute>
                               <IpdDocumentPage/>
                           </ProtectedRoute>
                    }
                />
            </Route>
        </Routes>
    );
}

export default App;