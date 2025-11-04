import React, { Component } from 'react';
import { Card, CardHeader, CardBody, Button, Container } from 'reactstrap';
import UserTable from './components/UserTable';
import UserForm from './components/UserForm';
import API_USERS from './api/user-api';

class UserContainer extends Component {
    constructor(props) {
        super(props);
        this.toggleForm = this.toggleForm.bind(this);
        this.reload = this.reload.bind(this);
        this.state = {
            showForm: false,
            tableData: [],
            isLoaded: false,
            errorStatus: 0,
            error: null
        };
    }

    componentDidMount() {
        this.fetchUsers();
    }

    fetchUsers() {
        const token = localStorage.getItem('token');

        if (!token) {
            alert('Please login first');
            window.location.href = '/login';
            return;
        }

        return API_USERS.getUsers(token, (result, status, err) => {
            if (result !== null && status === 200) {
                this.setState({
                    tableData: result,
                    isLoaded: true
                });
            } else {
                this.setState({
                    errorStatus: status,
                    error: err
                });
                if (status === 403 || status === 401) {
                    alert('Access denied. Only ADMIN can view users.');
                    window.location.href = '/';
                }
            }
        });
    }

    toggleForm() {
        this.setState({ showForm: !this.state.showForm });
    }

    reload() {
        this.setState({
            isLoaded: false,
            showForm: false
        });
        this.fetchUsers();
    }

    render() {
        const token = localStorage.getItem('token');

        return (
            <div className="page-container">
                <Card>
                    <CardHeader>
                        <strong>User Management</strong>
                    </CardHeader>
                    <CardBody>
                        <div className="action-buttons">
                            <Button color="primary" onClick={this.toggleForm}>
                                <i className="fas fa-plus"></i> Add User
                            </Button>
                        </div>

                        {this.state.isLoaded && (
                            <div className="table-container">
                                <UserTable
                                    tableData={this.state.tableData}
                                    reload={this.reload}
                                    token={token}
                                />
                            </div>
                        )}

                        {!this.state.isLoaded && this.state.errorStatus === 0 && (
                            <div className="loading">
                                <div className="spinner-border text-primary" role="status">
                                    <span className="visually-hidden">Loading...</span>
                                </div>
                                <p className="mt-3">Loading users...</p>
                            </div>
                        )}

                        {this.state.errorStatus > 0 && (
                            <div className="alert alert-danger">
                                <strong>Error {this.state.errorStatus}:</strong> {this.state.error?.message || 'Failed to load users'}
                            </div>
                        )}

                        {this.state.isLoaded && this.state.tableData.length === 0 && (
                            <div className="empty-state">
                                <i className="fas fa-users fa-4x"></i>
                                <h4>No users found</h4>
                                <p>Click "Add User" to create your first user.</p>
                            </div>
                        )}
                    </CardBody>
                </Card>

                {this.state.showForm && (
                    <UserForm
                        reloadHandler={this.reload}
                        toggleFormHandler={this.toggleForm}
                        token={token}
                    />
                )}
            </div>
        );
    }
}

export default UserContainer;