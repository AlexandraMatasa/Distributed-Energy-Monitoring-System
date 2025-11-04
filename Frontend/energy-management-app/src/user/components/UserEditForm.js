import React, { useState } from 'react';
import { Button, Form, FormGroup, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import API_USERS from '../api/user-api';

function UserEditForm(props) {
    const { user, token, reloadHandler, toggleFormHandler } = props;

    const [email, setEmail] = useState(user.email || '');
    const [fullName, setFullName] = useState(user.fullName || '');
    const [role, setRole] = useState(user.role || 'CLIENT');
    const [password, setPassword] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();

        setIsSubmitting(true);

        let updatedUser = {
            id: user.id,
            username: user.username,
            email: email,
            fullName: fullName,
            role: role
        };

        if (password && password.trim() !== '') {
            updatedUser.password = password;
        }
        else {
            updatedUser.password = "DUMMY_PASSWORD_TO_PASS_VALIDATION";
        }

        console.log('Updating user:', updatedUser);

        API_USERS.updateUser(token, updatedUser, (result, status, err) => {
            setIsSubmitting(false);

            if (status === 200 || status === 204) {
                console.log('Successfully updated user');
                alert('User updated successfully!');
                reloadHandler();
            } else {
                console.log('Error updating user:', err);
                alert('Failed to update user: ' + (err?.message || 'Unknown error'));
            }
        });
    };

    return (
        <Modal isOpen={true} toggle={toggleFormHandler} size="lg">
            <ModalHeader toggle={toggleFormHandler}>
                Edit User: {user.username}
            </ModalHeader>
            <ModalBody>
                <Form onSubmit={handleSubmit}>
                    <FormGroup>
                        <Label for="username">Username</Label>
                        <Input
                            id="username"
                            name="username"
                            type="text"
                            value={user.username}
                            disabled
                        />
                    </FormGroup>

                    <FormGroup>
                        <Label for="password">
                            New Password
                        </Label>
                        <Input
                            id="password"
                            name="password"
                            placeholder="Leave empty to keep current password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
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
                    {isSubmitting ? 'Updating...' : 'Update User'}
                </Button>
                <Button
                    color="secondary"
                    onClick={toggleFormHandler}
                    disabled={isSubmitting}
                >
                    Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );
}

export default UserEditForm;