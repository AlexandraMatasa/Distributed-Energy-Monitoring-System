import React, { useState } from 'react';
import { Table, Button } from 'reactstrap';
import API_USERS from '../api/user-api';
import UserEditForm from './UserEditForm';

function UserTable(props) {
    const { tableData, reload, token } = props;
    const [editingUser, setEditingUser] = useState(null);

    const deleteUser = (id, username) => {
        if (window.confirm(`Are you sure you want to delete user "${username}"?`)) {
            API_USERS.deleteUser(token, id, (result, status, err) => {
                if (status === 204 || status === 200) {
                    console.log('Successfully deleted user with id:', id);
                    alert('User deleted successfully!');
                    reload();
                } else {
                    console.log('Error deleting user:', err);
                    alert('Failed to delete user: ' + (err?.message || 'Unknown error'));
                }
            });
        }
    };

    const handleEdit = (user) => {
        setEditingUser(user);
    };

    const closeEditForm = () => {
        setEditingUser(null);
    };

    return (
        <>
            <Table striped responsive>
                <thead>
                <tr>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Full Name</th>
                    <th>Role</th>
                    <th style={{ width: '200px' }}>Actions</th>
                </tr>
                </thead>
                <tbody>
                {tableData.map((row, index) => (
                    <tr key={row.id || index}>
                        <td>
                            <strong>{row.username}</strong>
                        </td>
                        <td>{row.email || '-'}</td>
                        <td>{row.fullName || '-'}</td>
                        <td>
                            {row.role}
                        </td>
                        <td>
                            <Button
                                color="warning"
                                size="sm"
                                onClick={() => handleEdit(row)}
                                className="me-2"
                            >
                                Edit
                            </Button>
                            <Button
                                color="danger"
                                size="sm"
                                onClick={() => deleteUser(row.id, row.username)}
                            >
                                 Delete
                            </Button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </Table>

            {editingUser && (
                <UserEditForm
                    user={editingUser}
                    token={token}
                    reloadHandler={reload}
                    toggleFormHandler={closeEditForm}
                />
            )}
        </>
    );
}

export default UserTable;