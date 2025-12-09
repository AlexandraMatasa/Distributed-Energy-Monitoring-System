import React, { Component } from 'react';
import { Card, CardHeader, CardBody, Form, FormGroup, Label, Input, Button, Alert } from 'reactstrap';
import API_DEVICES from '../device/api/device-api';
import EnergyChart from './components/EnergyChart';
import AlertNotification from './components/AlertNotification';

class MonitoringContainer extends Component {
    constructor(props) {
        super(props);
        this.state = {
            devices: [],
            selectedDeviceId: '',
            selectedDevice: null,
            isLoading: true,
            error: null,
            alerts: [],
            alertIdCounter: 0
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
            window.location.href = '/login';
            return;
        }

        if (role === 'CLIENT') {
            API_DEVICES.getDevicesByUserId(token, userId, (result, status, err) => {
                if (result !== null && status === 200) {
                    this.setState({
                        devices: result,
                        isLoading: false,
                        selectedDeviceId: result.length > 0 ? result[0].id : '',
                        selectedDevice: result.length > 0 ? result[0] : null
                    });
                } else {
                    this.setState({
                        error: err?.message || 'Failed to load devices',
                        isLoading: false
                    });
                }
            });
        } else {
            API_DEVICES.getDevices(token, (result, status, err) => {
                if (result !== null && status === 200) {
                    this.setState({
                        devices: result,
                        isLoading: false,
                        selectedDeviceId: result.length > 0 ? result[0].id : '',
                        selectedDevice: result.length > 0 ? result[0] : null
                    });
                } else {
                    this.setState({
                        error: err?.message || 'Failed to load devices',
                        isLoading: false
                    });
                }
            });
        }
    }

    handleNewAlert = (alert) => {
        this.setState(prevState => ({
            alerts: [...prevState.alerts, { ...alert, id: prevState.alertIdCounter }],
            alertIdCounter: prevState.alertIdCounter + 1
        }));
    }

    dismissAlert = (id) => {
        this.setState(prevState => ({
            alerts: prevState.alerts.filter(a => a.id !== id)
        }));
    }

    handleDeviceChange = (e) => {
        const deviceId = e.target.value;
        const device = this.state.devices.find(d => d.id === deviceId);
        this.setState({
            selectedDeviceId: deviceId,
            selectedDevice: device
        });
    }

    render() {
        const { devices, selectedDeviceId, selectedDevice, isLoading, error, alerts } = this.state;
        const token = localStorage.getItem('token');
        const userId = localStorage.getItem('userId');
        return (
            <div className="page-container">
                {alerts.map(alert => (
                    <AlertNotification
                        key={alert.id}
                        alert={alert}
                        onDismiss={() => this.dismissAlert(alert.id)}
                    />
                ))}

                <Card>
                    <CardHeader>
                        <strong>Energy Consumption Monitoring</strong>
                    </CardHeader>
                    <CardBody>
                        {isLoading && (
                            <div className="text-center py-5">
                                <div className="spinner-border text-primary" role="status">
                                    <span className="visually-hidden">Loading...</span>
                                </div>
                                <p className="mt-3">Loading devices...</p>
                            </div>
                        )}

                        {error && (
                            <Alert color="danger">
                                {error}
                            </Alert>
                        )}

                        {!isLoading && devices.length === 0 && (
                            <div className="empty-state">
                                <i className="fas fa-chart-line fa-4x"></i>
                                <h4>No devices available</h4>
                                <p>You don't have any devices assigned to monitor.</p>
                            </div>
                        )}

                        {!isLoading && devices.length > 0 && (
                            <>
                                <Form className="mb-4">
                                    <FormGroup>
                                        <Label for="deviceSelect">
                                            <strong>Select Device to Monitor</strong>
                                        </Label>
                                        <Input
                                            id="deviceSelect"
                                            type="select"
                                            value={selectedDeviceId}
                                            onChange={this.handleDeviceChange}
                                        >
                                            {devices.map(device => (
                                                <option key={device.id} value={device.id}>
                                                    {device.name} - {device.maxConsumption}kW
                                                </option>
                                            ))}
                                        </Input>
                                    </FormGroup>
                                </Form>

                                {selectedDevice && (
                                    <EnergyChart
                                        device={selectedDevice}
                                        token={token}
                                        userId={userId}
                                        onAlert={this.handleNewAlert}
                                    />
                                )}
                            </>
                        )}
                    </CardBody>
                </Card>
            </div>
        );
    }
}

export default MonitoringContainer;