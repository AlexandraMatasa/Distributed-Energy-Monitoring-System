import React, { Component } from 'react';
import { Card, CardHeader, CardBody, Button } from 'reactstrap';
import API_USERS from '../user/api/user-api';
import API_DEVICES from '../device/api/device-api';
import AssignmentTable from './AssignmentTable';
import AssignDeviceForm from '../assignments/AssignDeviceForm';

class AssignmentsContainer extends Component {
    constructor(props) {
        super(props);
        this.state = {
            assignments: [],
            users: new Map(),
            isLoaded: false,
            errorStatus: 0,
            error: null,
            showAssignForm: false
        };
        this.reload = this.reload.bind(this);
        this.toggleAssignForm = this.toggleAssignForm.bind(this);
    }

    componentDidMount() {
        this.fetchAssignments();
    }

    fetchAssignments() {
        const token = localStorage.getItem('token');
        if (!token) {
            window.location.href = '/login';
            return;
        }

        API_USERS.getUsers(token, (users, status, err) => {
            if (users === null) {
                this.setState({ isLoaded: true, errorStatus: status, error: err });
                return;
            }

            const userMap = new Map();
            users.forEach(user => userMap.set(user.id, user));
            this.setState({ users: userMap });

            const promises = users.map(user =>
                new Promise(resolve => {
                    API_DEVICES.getDevicesByUserId(token, user.id, (devices, _, __) => {
                        resolve(devices || []);
                    });
                })
            );

            Promise.all(promises).then(allDeviceArrays => {
                const allAssignedDevices = allDeviceArrays.flat();
                this.setState({
                    assignments: allAssignedDevices,
                    isLoaded: true
                });
            });
        });
    }

    reload() {
        this.setState({
            isLoaded: false,
            assignments: [],
            showAssignForm: false
        });
        this.fetchAssignments();
    }

    toggleAssignForm() {
        this.setState({ showAssignForm: !this.state.showAssignForm });
    }

    render() {
        const { assignments, users, isLoaded, errorStatus, error, showAssignForm } = this.state;
        const token = localStorage.getItem('token');

        return (
            <div className="page-container">
                <Card>
                    <CardHeader>
                        <strong>Device Assignments</strong>
                    </CardHeader>
                    <CardBody>
                        <div className="action-buttons">
                            <Button color="success" onClick={this.toggleAssignForm}>
                                Assign Device to User
                            </Button>
                        </div>

                        {!isLoaded && errorStatus === 0 && (
                            <div className="loading">...Loading assignments...</div>
                        )}

                        {errorStatus > 0 && (
                            <div className="alert alert-danger">
                                Error: {error?.message}
                            </div>
                        )}

                        {isLoaded && assignments.length === 0 && errorStatus === 0 && (
                            <div className="empty-state">
                                <h4>No devices are currently assigned.</h4>
                                <p>Click "Assign Device to User" to create one.</p>
                            </div>
                        )}

                        {isLoaded && assignments.length > 0 && (
                            <div className="table-container">
                                <AssignmentTable
                                    assignments={assignments}
                                    userMap={users}
                                    token={token}
                                    reload={this.reload}
                                />
                            </div>
                        )}
                    </CardBody>
                </Card>

                {showAssignForm && (
                    <AssignDeviceForm
                        reloadHandler={this.reload}
                        toggleFormHandler={this.toggleAssignForm}
                        token={token}
                    />
                )}
            </div>
        );
    }
}

export default AssignmentsContainer;