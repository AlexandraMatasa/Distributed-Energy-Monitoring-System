class WebSocketService {
    constructor() {
        this.ws = null;
        this.listeners = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.currentDeviceId = null;
    }

    connect(deviceId) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            console.log('WebSocket already connected');
            if (this.currentDeviceId !== deviceId) {
                this.currentDeviceId = deviceId;
                this.send({
                    action: 'subscribe',
                    deviceId: deviceId
                });
            }
            return;
        }

        if (this.ws) {
            try {
                this.ws.close();
            } catch (e) {
                console.warn('Error closing existing connection:', e);
            }
        }

        this.currentDeviceId = deviceId;

        const wsUrl = 'ws://localhost:8084/ws/monitoring';
        console.log('🔌 Connecting to WebSocket:', wsUrl);
        console.log('🔌 Device ID:', deviceId);

        try {
            this.ws = new WebSocket(wsUrl);

            this.ws.onopen = () => {
                console.log('WebSocket connected successfully!');
                this.reconnectAttempts = 0;

                this.send({
                    action: 'subscribe',
                    deviceId: deviceId
                });
            };

            this.ws.onmessage = (event) => {
                try {
                    const message = JSON.parse(event.data);
                    console.log('WebSocket message received:', message);

                    if (message.type === 'newMeasurement') {
                        this.notifyListeners('newMeasurement', message);
                    } else if (message.type === 'subscribed') {
                        console.log('Successfully subscribed to device:', message.deviceId);
                        this.notifyListeners('subscribed', message);
                    } else if (message.type === 'error') {
                        console.error('Server error:', message);
                        this.notifyListeners('error', message);
                    }
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error, event.data);
                }
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.notifyListeners('error', { error });
            };

            this.ws.onclose = (event) => {
                console.log('WebSocket closed. Code:', event.code, 'Reason:', event.reason || 'No reason');
                this.notifyListeners('closed', { event });

                if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    console.log(`Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
                    setTimeout(() => {
                        if (this.currentDeviceId) {
                            this.connect(this.currentDeviceId);
                        }
                    }, this.reconnectDelay);
                } else if (this.reconnectAttempts >= this.maxReconnectAttempts) {
                    console.error('Max reconnection attempts reached');
                    this.notifyListeners('error', { message: 'Max reconnection attempts reached' });
                }
            };
        } catch (error) {
            console.error('Failed to create WebSocket:', error);
            this.notifyListeners('error', { error });
        }
    }

    send(data) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            const json = JSON.stringify(data);
            this.ws.send(json);
            console.log('Sent:', json);
        } else {
            console.warn('WebSocket not open. ReadyState:', this.ws?.readyState);
            console.warn('0=CONNECTING, 1=OPEN, 2=CLOSING, 3=CLOSED');
        }
    }

    addListener(id, callback) {
        this.listeners.set(id, callback);
        console.log(`Added listener: ${id} (Total: ${this.listeners.size})`);
    }

    removeListener(id) {
        const removed = this.listeners.delete(id);
    }

    notifyListeners(type, data) {
        console.log(`Notifying ${this.listeners.size} listeners: ${type}`);
        this.listeners.forEach((callback, id) => {
            try {
                callback(type, data);
            } catch (error) {
                console.error(`Error in listener ${id}:`, error);
            }
        });
    }

    disconnect() {
        if (this.ws) {
            console.log('Disconnecting WebSocket...');
            try {
                this.ws.close(1000, 'Client disconnect');
            } catch (e) {
                console.warn('Error during disconnect:', e);
            }
            this.ws = null;
        }
        this.listeners.clear();
        this.reconnectAttempts = 0;
        this.currentDeviceId = null;
    }

    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }
}

export default new WebSocketService();