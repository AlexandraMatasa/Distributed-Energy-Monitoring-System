import React from 'react';
import { Container, Button } from 'reactstrap';

function Home() {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    return (
        <div className="home-wrapper">
            <div className="jumbotron" style={{ backgroundColor: '#17a2b8', color: 'white', padding: '60px' }}>
                <Container fluid>
                    <h1 className="display-3">Energy Management System</h1>
                    <p className="lead">
                        Manage users, devices, and monitor energy consumption in real-time.
                    </p>
                    <hr className="my-2" style={{ borderColor: 'white' }} />
                    {token ? (
                        <div>
                            <p>Welcome! You are logged in as <strong>{role}</strong></p>
                            <p>
                                {role === 'ADMIN'
                                    ? 'You can manage users and devices.'
                                    : 'You can view your assigned devices.'}
                            </p>
                        </div>
                    ) : (
                        <div>
                            <p>Please login to access the system.</p>
                            <p className="lead">
                                <Button color="primary" href="/login">
                                    Login
                                </Button>
                            </p>
                        </div>
                    )}
                </Container>
            </div>
        </div>
    );
}

export default Home;