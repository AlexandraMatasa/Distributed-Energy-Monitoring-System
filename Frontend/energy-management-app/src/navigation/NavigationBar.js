import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import {
    Collapse,
    Navbar,
    NavbarToggler,
    NavbarBrand,
    Nav,
    NavItem,
    NavLink,
    Button,
    Container
} from 'reactstrap';

function NavigationBar() {
    const [isOpen, setIsOpen] = useState(false);
    const [authState, setAuthState] = useState({
        token: localStorage.getItem('token'),
        role: localStorage.getItem('role'),
        username: localStorage.getItem('username')
    });

    const location = useLocation();

    useEffect(() => {
        const updateAuthState = () => {
            setAuthState({
                token: localStorage.getItem('token'),
                role: localStorage.getItem('role'),
                username: localStorage.getItem('username')
            });
        };

        updateAuthState();

        window.addEventListener('storage', updateAuthState);

        return () => {
            window.removeEventListener('storage', updateAuthState);
        };
    }, [location]);

    const toggle = () => setIsOpen(!isOpen);

    const { token, role, username } = authState;

    const handleLogout = () => {
        if (window.confirm('Are you sure you want to logout?')) {
            localStorage.removeItem('token');
            localStorage.removeItem('role');
            localStorage.removeItem('userId');
            localStorage.removeItem('username');
            window.location.href = '/';
        }
    };

    return (
        <Navbar color="dark" dark expand="md" className="navbar">
            <Container fluid>
                <NavbarBrand href="/">
                    Energy Management
                </NavbarBrand>
                <NavbarToggler onClick={toggle} />
                <Collapse isOpen={isOpen} navbar>
                    <Nav className="ms-auto" navbar>
                        <NavItem>
                            <NavLink href="/">Home</NavLink>
                        </NavItem>

                        {token && role === 'ADMIN' && (
                            <>
                                <NavItem>
                                    <NavLink href="/users">Users</NavLink>
                                </NavItem>
                                <NavItem>
                                    <NavLink href="/devices">Devices</NavLink>
                                </NavItem>
                                <NavItem>
                                    <NavLink href="/assignments">Assignments</NavLink>
                                </NavItem>
                            </>
                        )}

                        {token && role === 'CLIENT' && (
                            <>
                                <NavItem>
                                    <NavLink href="/devices">My Devices</NavLink>
                                </NavItem>
                                <NavItem>
                                    <NavLink href="/monitoring">
                                        Monitoring
                                    </NavLink>
                                </NavItem>
                            </>
                        )}

                        {token ? (
                            <>
                                <NavItem className="d-none d-md-block">
                                    <NavLink disabled style={{ color: '#adb5bd' }}>
                                        <strong>User: </strong>{username || role}
                                    </NavLink>
                                </NavItem>
                                <NavItem>
                                    <Button
                                        color="link"
                                        onClick={handleLogout}
                                        style={{ color: '#f8f9fa', textDecoration: 'none' }}
                                        className="nav-link"
                                    >
                                        Logout
                                    </Button>
                                </NavItem>
                            </>
                        ) : (
                            <NavItem>
                                <NavLink href="/login">Login</NavLink>
                            </NavItem>
                        )}
                    </Nav>
                </Collapse>
            </Container>
        </Navbar>
    );
}

export default NavigationBar;