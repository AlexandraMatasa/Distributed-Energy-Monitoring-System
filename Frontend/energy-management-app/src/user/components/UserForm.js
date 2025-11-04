import React, { useState } from 'react';
import { Button, Form, FormGroup, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import API_USERS from '../api/user-api';

function UserForm(props) {
    const { token, reloadHandler, toggleFormHandler } = props;

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [email, setEmail] = useState('');
    const [fullName, setFullName] = useState('');
    const [role, setRole] = useState('CLIENT');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!username || !password) {
            alert('Username and Password are required!');
            return;
        }

        setIsSubmitting(true);

        let user = {
            username: username,
            password: password,
            email: email,
            fullName: fullName,
            role: role
        };

        console.log('Creating user:', user);

        API_USERS.postUser(token, user, (result, status, err) => {
            setIsSubmitting(false);

            if (status === 201 || status === 200) {
                console.log('Successfully created user:', result);
                alert(result?.message || 'User created successfully!');
                reloadHandler();
                toggleFormHandler();
            }
             else if (status === 403) {
                console.log('Forbidden:', err);
                alert('Access denied: ' + (err?.message || 'Only ADMIN can register new users'));
            } else {
                console.log('Error creating user:', err);
                alert('Failed to create user: ' + (err?.message || 'Unknown error'));
            }
        });
    };

    return (
        <Modal isOpen={true} toggle={toggleFormHandler} size="lg">
            <ModalHeader toggle={toggleFormHandler}>
                Add New User
            </ModalHeader>
            <ModalBody>
                <Form onSubmit={handleSubmit}>
                    <FormGroup>
                        <Label for="username">
                            Username <span className="text-danger">*</span>
                        </Label>
                        <Input
                            id="username"
                            name="username"
                            placeholder="Enter username"
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            disabled={isSubmitting}
                            autoFocus
                        />
                    </FormGroup>

                    <FormGroup>
                        <Label for="password">
                            Password <span className="text-danger">*</span>
                        </Label>
                        <Input
                            id="password"
                            name="password"
                            placeholder="Enter password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            disabled={isSubmitting}
                        />
                    </FormGroup>

                    <FormGroup>
                        <Label for="email">Email</Label>
                        <Input
                            id="email"
                            name="email"
                            placeholder="Enter email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            disabled={isSubmitting}
                        />
                    </FormGroup>

                    <FormGroup>
                        <Label for="fullName">Full Name</Label>
                        <Input
                            id="fullName"
                            name="fullName"
                            placeholder="Enter full name"
                            type="text"
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                            disabled={isSubmitting}
                        />
                    </FormGroup>

                    <FormGroup>
                        <Label for="role">
                            Role <span className="text-danger">*</span>
                        </Label>
                        <Input
                            id="role"
                            name="role"
                            type="select"
                            value={role}
                            onChange={(e) => setRole(e.target.value)}
                            disabled={isSubmitting}
                        >
                            <option value="CLIENT">CLIENT</option>
                            <option value="ADMIN">ADMIN</option>
                        </Input>
                    </FormGroup>
                </Form>
            </ModalBody>
            <ModalFooter>
                <Button
                    color="primary"
                    onClick={handleSubmit}
                    disabled={isSubmitting}
                >
                    {isSubmitting ? 'Creating...' : 'Create User'}
                </Button>
                <Button
                    color="secondary"
                    onClick={toggleFormHandler}
                    disabled={isSubmitting}
                >
                    <i className="fas fa-times"></i> Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );
}

export default UserForm;