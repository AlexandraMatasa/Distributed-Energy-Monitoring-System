import React, { Component } from 'react';
import { Card, CardHeader, CardBody, Button } from 'reactstrap';
import DeviceTable from './components/DeviceTable';
import DeviceForm from './components/DeviceForm';
import API_DEVICES from './api/device-api';

class DeviceContainer extends Component {
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
        this.fetchDevices();
    }

    fetchDevices() {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('role');
        const userId = localStorage.getItem('userId');

        if (!token) {
            alert('Please login first');
            window.location.href = '/login';
            return;
        }

        if (role === 'CLIENT') {
            return API_DEVICES.getDevicesByUserId(token, userId, (result, status, err) => {
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
                }
            });
        }

        return API_DEVICES.getDevices(token, (result, status, err) => {
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
                    alert('Access denied.');
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
            showForm: false,
        });
        this.fetchDevices();
    }

    render() {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('role');

        return (
            <div className="page-container">
                <Card>
                    <CardHeader>
                        <strong>{role === 'ADMIN' ? 'Device Management' : 'My Devices'}</strong>
                    </CardHeader>
                    <CardBody>
                        {role === 'ADMIN' && (
                            <div className="action-buttons">
                                <Button color="primary" onClick={this.toggleForm}>
                                    <i className="fas fa-plus"></i> Add Device
                                </Button>
                            </div>
                        )}

                        {this.state.isLoaded && (
                            <div className="table-container">
                                <DeviceTable
                                    tableData={this.state.tableData}
                                    reload={this.reload}
                                    token={token}
                                    role={role}
                                />
                            </div>
                        )}

                        {!this.state.isLoaded && this.state.errorStatus === 0 && (
                            <div className="loading">
                                <div className="spinner-border text-primary" role="status">
                                    <span className="visually-hidden">Loading...</span>
                                </div>
                                <p className="mt-3">Loading devices...</p>
                            </div>
                        )}

                        {this.state.errorStatus > 0 && (
                            <div className="alert alert-danger">
                                <strong>Error {this.state.errorStatus}:</strong> {this.state.error?.message || 'Failed to load devices'}
                            </div>
                        )}

                        {this.state.isLoaded && this.state.tableData.length === 0 && (
                            <div className="empty-state">
                                <i className="fas fa-microchip fa-4x"></i>
                                <h4>No devices found</h4>
                                <p>
                                    {role === 'ADMIN'
                                        ? 'Click "Add Device" to create your first device.'
                                        : 'No devices have been assigned to you yet.'}
                                </p>
                            </div>
                        )}
                    </CardBody>
                </Card>

                {this.state.showForm && (
                    <DeviceForm
                        reloadHandler={this.reload}
                        toggleFormHandler={this.toggleForm}
                        token={token}
                    />
                )}
            </div>
        );
    }
}

export default DeviceContainer;