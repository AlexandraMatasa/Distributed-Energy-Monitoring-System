import React, { useState, useEffect } from 'react';
import { Toast, ToastBody, ToastHeader } from 'reactstrap';
import './AlertNotification.css';

function AlertNotification({ alert, onDismiss }) {
    const [show, setShow] = useState(true);

    useEffect(() => {
        const timer = setTimeout(() => {
            setShow(false);
            setTimeout(onDismiss, 300);
        }, 10000); // Auto-dismiss după 10 secunde

        return () => clearTimeout(timer);
    }, [onDismiss]);

    if (!show) return null;

    return (
        <Toast className="alert-toast">
            <ToastHeader
                icon={<span className="text-danger">⚠️</span>}
                toggle={() => {
                    setShow(false);
                    setTimeout(onDismiss, 300);
                }}
            >
                Overconsumption Alert
            </ToastHeader>
            <ToastBody>
                <strong>{alert.deviceName}</strong>
                <p className="mb-1">{alert.message}</p>
                <small className="text-muted">
                    {new Date(alert.timestamp).toLocaleString()}
                </small>
            </ToastBody>
        </Toast>
    );
}

export default AlertNotification;