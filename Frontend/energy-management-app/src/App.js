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

function App() {
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
                </Routes>
            </div>
        </Router>
    );
}

export default App;