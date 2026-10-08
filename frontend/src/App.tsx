import {Route, Routes} from 'react-router-dom';
import {Layout} from './components/Layout';
import {Home} from './pages/Home';
import {TicketsPage} from './pages/TicketsPage';
import {TaskPlannerPage} from './pages/TaskPlannerPage';
import {ProtectedRoute} from './auth/ProtectedRoute';
import {ChecklistsPage} from './pages/ChecklistsPage';
import {IpdGeneratorPage} from './pages/IpdGeneratorPage';
import {IpdDocumentPage} from './pages/IpdDocumentPage';
import './App.css';

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
                    path="/checklists"
                    element={
                        <ProtectedRoute>
                            <ChecklistsPage/>
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
