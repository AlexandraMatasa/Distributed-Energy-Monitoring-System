import React, { useState } from 'react';
import { Button, Form, FormGroup, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import API_DEVICES from '../api/device-api';

function DeviceEditForm(props) {
    const { device, token, reloadHandler, toggleFormHandler } = props;

    const [name, setName] = useState(device.name || '');
    const [description, setDescription] = useState(device.description || '');
    const [maxConsumption, setMaxConsumption] = useState(device.maxConsumption || '');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!name || !maxConsumption) {
            alert('Device Name and Max Consumption are required!');
            return;
        }

        if (parseFloat(maxConsumption) <= 0) {
            alert('Max Consumption must be greater than 0!');
            return;
        }

        setIsSubmitting(true);

        let updatedDevice = {
            id: device.id,
            name: name,
            description: description,
            maxConsumption: parseFloat(maxConsumption)
        };

        console.log('Updating device:', updatedDevice);

        API_DEVICES.updateDevice(token, updatedDevice, (result, status, err) => {
            setIsSubmitting(false);

            if (status === 200 || status === 204) {
                console.log('Successfully updated device');
                alert('Device updated successfully!');
                reloadHandler();
            } else {
                console.log('Error updating device:', err);
                alert('Failed to update device: ' + (err?.message || 'Unknown error'));
            }
        });
    };

    return (
        <Modal isOpen={true} toggle={toggleFormHandler} size="lg">
            <ModalHeader toggle={toggleFormHandler}>
                Edit Device: {device.name}
            </ModalHeader>
            <ModalBody>
                <Form onSubmit={handleSubmit}>
                    <FormGroup>
                        <Label for="name">
                            Device Name <span className="text-danger">*</span>
                        </Label>
                        <Input
                            id="name"
                            name="name"
                            placeholder="Enter device name (e.g., Smart Thermostat)"
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                            disabled={isSubmitting}
                        />
                    </FormGroup>

                    <FormGroup>
                        <Label for="description">Description</Label>
                        <Input
                            id="description"
                            name="description"
                            placeholder="Enter device description"
                            type="textarea"
                            rows="3"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            disabled={isSubmitting}
                        />
                    </FormGroup>

                    <FormGroup>
                        <Label for="maxConsumption">
                            Maximum Hourly Consumption (kW) <span className="text-danger">*</span>
                        </Label>
                        <Input
                            id="maxConsumption"
                            name="maxConsumption"
                            placeholder="Enter max consumption in kW (e.g., 0.5 for 500W)"
                            type="number"
                            step="0.01"
                            min="0.01"
                            value={maxConsumption}
                            onChange={(e) => setMaxConsumption(e.target.value)}
                            required
                            disabled={isSubmitting}
                        />
                    </FormGroup>
                </Form>
            </ModalBody>
            <ModalFooter>
                <Button
                    color="primary"
                    onClick={handleSubmit}
                    disabled={isSubmitting}
                >
                    {isSubmitting ? 'Updating...' : 'Update Device'}
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

export default DeviceEditForm;