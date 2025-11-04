import React from 'react';
import { Table, Button } from 'reactstrap';
import API_DEVICES from '../device/api/device-api';

function AssignmentTable(props) {
    const { assignments, userMap, token, reload } = props;

    const handleUnassign = (deviceId, deviceName) => {
        if (window.confirm(`Are you sure you want to unassign device "${deviceName}"?`)) {
            API_DEVICES.unassignDevice(token, deviceId, (result, status, err) => {
                if (status === 204 || status === 200) {
                    alert('Device unassigned successfully!');
                    reload();
                } else {
                    alert('Failed to unassign device: ' + (err?.message || 'Error'));
                }
            });
        }
    };

    return (
        <Table striped responsive>
            <thead>
            <tr>
                <th>Device Name</th>
                <th>Max Consumption</th>
                <th>Assigned To User</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {assignments.map(device => {
                const user = userMap.get(device.userId);
                const userName = user ? user.username : 'Unknown User';

                return (
                    <tr key={device.id}>
                        <td><strong>{device.name}</strong></td>
                        <td>
                            {device.maxConsumption}
                        </td>
                        <td>
                            {userName}
                        </td>
                        <td>
                            <Button
                                color="info"
                                size="sm"
                                className="text-white"
                                onClick={() => handleUnassign(device.id, device.name)}
                            >
                               Unassign
                            </Button>
                        </td>
                    </tr>
                );
            })}
            </tbody>
        </Table>
    );
}

export default AssignmentTable;