import React, { useState } from 'react';
import { Table, Button } from 'reactstrap';
import API_DEVICES from '../api/device-api';
import DeviceEditForm from './DeviceEditForm';

function DeviceTable(props) {
    const { tableData, reload, token, role } = props;
    const [editingDevice, setEditingDevice] = useState(null);

    const deleteDevice = (id, name) => {
        if (window.confirm(`Are you sure you want to delete device "${name}"?`)) {
            API_DEVICES.deleteDevice(token, id, (result, status, err) => {
                if (status === 204 || status === 200) {
                    console.log('Successfully deleted device with id:', id);
                    alert('Device deleted successfully!');
                    reload();
                } else {
                    console.log('Error deleting device:', err);
                    alert('Failed to delete device: ' + (err?.message || 'Unknown error'));
                }
            });
        }
    };

    const handleEdit = (device) => {
        setEditingDevice(device);
    };

    const closeEditForm = () => {
        setEditingDevice(null);
    };

    return (
        <>
            <Table striped responsive>
                <thead>
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                    <th style={{ width: '180px' }}>Max Consumption</th>
                    {role === 'ADMIN' && <th style={{ width: '200px' }}>Actions</th>}
                </tr>
                </thead>
                <tbody>
                {tableData.map((row, index) => (
                    <tr key={row.id || index}>
                        <td>
                            <strong>{row.name}</strong>
                        </td>
                        <td>{row.description || '-'}</td>
                        <td>
                            {row.maxConsumption} W
                        </td>

                        {role === 'ADMIN' && (
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
                                    onClick={() => deleteDevice(row.id, row.name)}
                                >
                                    Delete
                                </Button>
                            </td>
                        )}
                    </tr>
                ))}
                </tbody>
            </Table>

            {editingDevice && (
                <DeviceEditForm
                    device={editingDevice}
                    token={token}
                    reloadHandler={reload}
                    toggleFormHandler={closeEditForm}
                />
            )}
        </>
    );
}

export default DeviceTable;