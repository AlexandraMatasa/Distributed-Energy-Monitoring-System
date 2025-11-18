import React from 'react';
import { Container, Card, CardBody, Button } from 'reactstrap';

function AccessDenied() {
    const role = localStorage.getItem('role');

    return (
        <div className="page-container">
            <Container>
                <Card>
                    <CardBody className="text-center" style={{ padding: '60px 20px' }}>
                        <h2 className="mb-3">Access Denied</h2>
                        <p className="lead mb-4">
                            You don't have permission to access this page.
                        </p>
                        <p className="text-muted mb-4">
                            Your current role: <strong>{role || 'Unknown'}</strong>
                        </p>
                        <div className="d-flex gap-2 justify-content-center">
                            <Button
                                color="primary"
                                href="/"
                            >
                                Go to Home
                            </Button>
                            {role === 'CLIENT' && (
                                <Button
                                    color="info"
                                    href="/devices"
                                >
                                    My Devices
                                </Button>
                            )}
                        </div>
                    </CardBody>
                </Card>
            </Container>
        </div>
    );
}

export default AccessDenied;