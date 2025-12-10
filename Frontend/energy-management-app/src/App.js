import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import NavigationBar from './navigation/NavigationBar';
import Home from './home/Home';
import Login from './navigation/Login';
import UserContainer from './user/UserContainer';
import DeviceContainer from './device/DeviceContainer';
import AssignmentsContainer from './assignments/AssignmentsContainer';
import AccessDenied from './navigation/AccessDenied';
import ProtectedRoute from './navigation/ProtectedRoute';
import MonitoringContainer from './monitoring/MonitoringContainer';
import ChatWidget from './monitoring/components/ChatWidget';
import AdminChatPanel from './admin/AdminChatPanel';

function App() {
    const role = localStorage.getItem('role');

    return (
        <Router>
            <div className="App">
                <NavigationBar />
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/access-denied" element={<AccessDenied />} />

                    <Route
                        path="/users"
                        element={
                            <ProtectedRoute allowedRoles={['ADMIN']}>
                                <UserContainer />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/assignments"
                        element={
                            <ProtectedRoute allowedRoles={['ADMIN']}>
                                <AssignmentsContainer />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/devices"
                        element={
                            <ProtectedRoute allowedRoles={['ADMIN', 'CLIENT']}>
                                <DeviceContainer />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/monitoring"
                        element={
                            <ProtectedRoute allowedRoles={['ADMIN', 'CLIENT']}>
                                <MonitoringContainer />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/admin-chat"
                        element={
                            <ProtectedRoute allowedRoles={['ADMIN']}>
                                <AdminChatPanel />
                            </ProtectedRoute>
                        }
                    />
                </Routes>

                {role === 'CLIENT' && <ChatWidget />}
            </div>
        </Router>
    );
}

export default App;