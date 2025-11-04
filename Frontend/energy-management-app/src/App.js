import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import NavigationBar from './navigation/NavigationBar';
import Home from './home/Home';
import Login from './navigation/Login';
import UserContainer from './user/UserContainer';
import DeviceContainer from './device/DeviceContainer';
import AssignmentsContainer from './assignments/AssignmentsContainer';
// import './App.css';

function App() {
  return (
      <Router>
        <div className="App">
          <NavigationBar />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/users" element={<UserContainer />} />
            <Route path="/devices" element={<DeviceContainer />} />
            <Route path="/assignments" element={<AssignmentsContainer />} />
          </Routes>
        </div>
      </Router>
  );
}

export default App;