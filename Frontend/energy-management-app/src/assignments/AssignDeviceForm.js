import React, { useState, useEffect } from 'react';
import { Button, Form, FormGroup, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import API_DEVICES from '../device/api/device-api';
import API_USERS from '../user/api/user-api';

function AssignDeviceForm(props) {
    const { token, reloadHandler, toggleFormHandler } = props;

    const [devices, setDevices] = useState([]);
    const [users, setUsers] = useState([]);
    const [selectedDevice, setSelectedDevice] = useState('');
    const [selectedUser, setSelectedUser] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            new Promise((resolve) => {
                API_DEVICES.getDevices(token, (result, status, err) => {
                    if (result !== null && status === 200) {
                        setDevices(result);
                    }
                    resolve();
                });
            }),
            new Promise((resolve) => {
                API_USERS.getUsers(token, (result, status, err) => {
                    if (result !== null && status === 200) {
                        setUsers(result);
                    }
                    resolve();
                });
            })
        ]).then(() => {
            setIsLoading(false);
        });
    }, [token]);

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!selectedDevice || !selectedUser) {
            alert('Please select both device and user!');
            return;
        }

        setIsSubmitting(true);

        let assignment = {
            deviceId: selectedDevice,
            userId: selectedUser
        };

        console.log('Assigning device:', assignment);

        API_DEVICES.assignDeviceToUser(token, assignment, (result, status, err) => {
            setIsSubmitting(false);

            if (status === 204 || status === 200) {
                console.log('Successfully assigned device');
                alert('Device assigned successfully!');
                reloadHandler();
            } else {
                console.log('Error assigning device:', err);
                alert('Failed to assign device: ' + (err?.message || 'Unknown error'));
            }
        });
    };

    const selectedDeviceObj = devices.find(d => d.id === selectedDevice);
    const selectedUserObj = users.find(u => u.id === selectedUser);

    return (
        <Modal isOpen={true} toggle={toggleFormHandler} size="lg">
            <ModalHeader toggle={toggleFormHandler}>
                Assign Device to User
            </ModalHeader>
            <ModalBody>
                {isLoading ? (
                    <div className="text-center py-5">
                        <div className="text-center py-5">
                            <p>Loading devices and users...</p>
                        </div>
                    </div>
                ) : (
                    <Form onSubmit={handleSubmit}>
                        <FormGroup>
                        <Label for="device">
                                Select Device <span className="text-danger">*</span>
                            </Label>
                            <Input
                                id="device"
                                name="device"
                                type="select"
                                value={selectedDevice}
                                onChange={(e) => setSelectedDevice(e.target.value)}
                                disabled={isSubmitting}
                                required
                            >
                                <option value="">-- Select a Device --</option>
                                {devices.map((device) => (
                                    <option key={device.id} value={device.id}>
                                        {device.name} ({device.maxConsumption}W)
                                    </option>
                                ))}
                            </Input>
                            {devices.length === 0 && (
                                <small className="form-text text-danger">
                                    No devices available. Please create a device first.
                                </small>
                            )}
                        </FormGroup>

                        <FormGroup>
                            <Label for="user">
                                Select User <span className="text-danger">*</span>
                            </Label>
                            <Input
                                id="user"
                                name="user"
                                type="select"
                                value={selectedUser}
                                onChange={(e) => setSelectedUser(e.target.value)}
                                disabled={isSubmitting}
                                required
                            >
                                <option value="">-- Select a User --</option>
                                {users.map((user) => (
                                    <option key={user.id} value={user.id}>
                                        {user.username} - {user.fullName} ({user.role})
                                    </option>
                                ))}
                            </Input>
                            {users.length === 0 && (
                                <small className="form-text text-danger">
                                    No users available. Please create a user first.
                                </small>
                            )}
                        </FormGroup>

                        {selectedDeviceObj && selectedUserObj && (
                            <div className="alert alert-info">
                                <strong>Assignment Summary:</strong>
                                <br />
                                Device: <strong>{selectedDeviceObj.name}</strong> ({selectedDeviceObj.maxConsumption}W)
                                <br />
                                User: <strong>{selectedUserObj.username}</strong> ({selectedUserObj.fullName})
                            </div>
                        )}
                    </Form>
                )}
            </ModalBody>
            <ModalFooter>
                <Button
                    color="success"
                    onClick={handleSubmit}
                    disabled={isSubmitting || isLoading || devices.length === 0 || users.length === 0}
                >
                    {isSubmitting ? 'Assigning...' : 'Assign Device'}
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

export default AssignDeviceForm;