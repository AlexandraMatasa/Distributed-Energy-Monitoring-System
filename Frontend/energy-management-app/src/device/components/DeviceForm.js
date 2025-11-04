import React, { useState } from 'react';
import { Button, Form, FormGroup, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';
import API_DEVICES from '../api/device-api';

function DeviceForm(props) {
    const { token, reloadHandler, toggleFormHandler } = props;

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [maxConsumption, setMaxConsumption] = useState('');
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

        let device = {
            name: name,
            description: description,
            maxConsumption: parseFloat(maxConsumption)
        };

        console.log('Creating device:', device);

        API_DEVICES.postDevice(token, device, (result, status, err) => {
            setIsSubmitting(false);

            if (result !== null || status === 201) {
                console.log('Successfully created device');
                alert('Device created successfully!');
                reloadHandler();
            } else {
                console.log('Error creating device:', err);
                alert('Failed to create device: ' + (err?.message || 'Unknown error'));
            }
        });
    };

    return (
        <Modal isOpen={true} toggle={toggleFormHandler} size="lg">
            <ModalHeader toggle={toggleFormHandler}>
                Add New Device
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
                            placeholder="Enter device name"
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
                            Max Consumption (Watts) <span className="text-danger">*</span>
                        </Label>
                        <Input
                            id="maxConsumption"
                            name="maxConsumption"
                            placeholder="Enter max consumption in watts"
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
                    {isSubmitting ? 'Creating...' : 'Create Device'}
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

export default DeviceForm;