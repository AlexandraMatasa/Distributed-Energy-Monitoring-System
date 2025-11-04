import React, { useState } from 'react';
import { Form, FormGroup, Label, Input, Button, Card, CardBody, CardHeader, Alert } from 'reactstrap';
import API_AUTH from '../commons/api/auth-api';

function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const credentials = {
            username: username,
            password: password
        };

        API_AUTH.login(credentials, (result, status, err) => {
            setIsLoading(false);

            if (result !== null && status === 200) {
                console.log('Login successful:', result);

                localStorage.setItem('token', result.token);
                localStorage.setItem('role', result.role);
                localStorage.setItem('userId', result.userId);
                localStorage.setItem('username', username);

                window.location.href = '/';
            } else {
                console.log('Login failed:', err);
                setError(err?.message || 'Invalid username or password');
            }
        });
    };

    return (
        <div className="login-container">
            <Card className="login-card">
                <CardHeader>
                    <h4>
                       Energy Management System
                    </h4>
                    <p className="mb-0 mt-2" style={{ fontSize: '0.9rem', opacity: 0.9 }}>
                        Please login to continue
                    </p>
                </CardHeader>
                <CardBody>
                    {error && (
                        <Alert color="danger">
                            {error}
                        </Alert>
                    )}
                    <Form onSubmit={handleSubmit}>
                        <FormGroup>
                            <Label for="username">
                               Username
                            </Label>
                            <Input
                                id="username"
                                name="username"
                                placeholder="Enter your username"
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                                disabled={isLoading}
                                autoFocus
                            />
                        </FormGroup>
                        <FormGroup>
                            <Label for="password">
                                Password
                            </Label>
                            <Input
                                id="password"
                                name="password"
                                placeholder="Enter your password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isLoading}
                            />
                        </FormGroup>
                        <Button
                            color="primary"
                            type="submit"
                            block
                            size="lg"
                            disabled={isLoading}
                            className="mt-3"
                        >
                            {isLoading ? (
                                'Logging in...'
                            ) : (
                                'Login'
                            )}
                        </Button>
                    </Form>
                </CardBody>
            </Card>
        </div>
    );
}

export default Login;