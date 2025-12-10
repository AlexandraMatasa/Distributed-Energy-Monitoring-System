class ChatService {
    constructor() {
        this.ws = null;
        this.listeners = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.userId = null;
        this.username = null;
        this.role = null;
    }

    connect(userId, username, role) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            console.log('Chat WebSocket already connected');
            return;
        }

        this.userId = userId;
        this.username = username;
        this.role = role;

        const wsUrl = 'ws://localhost:8085/ws/chat';
        console.log('Connecting to Chat WebSocket:', wsUrl);

        try {
            this.ws = new WebSocket(wsUrl);

            this.ws.onopen = () => {
                console.log('Chat WebSocket connected!');
                this.reconnectAttempts = 0;

                this.send({
                    action: 'register',
                    userId: this.userId,
                    username: this.username,
                    role: this.role
                });
            };

            this.ws.onmessage = (event) => {
                try {
                    const message = JSON.parse(event.data);
                    console.log('Chat message received:', message);

                    this.notifyListeners(message.type, message);
                } catch (error) {
                    console.error('Error parsing chat message:', error);
                }
            };

            this.ws.onerror = (error) => {
                console.error('Chat WebSocket error:', error);
                this.notifyListeners('error', { error });
            };

            this.ws.onclose = (event) => {
                console.log('Chat WebSocket closed');
                this.notifyListeners('closed', { event });

                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    setTimeout(() => {
                        if (this.userId && this.username && this.role) {
                            this.connect(this.userId, this.username, this.role);
                        }
                    }, this.reconnectDelay);
                }
            };
        } catch (error) {
            console.error('Failed to create Chat WebSocket:', error);
        }
    }

    sendMessage(message, targetUserId = null) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            const payload = {
                action: 'message',
                userId: this.userId,
                username: this.username,
                role: this.role,
                message: message
            };

            if (targetUserId) {
                payload.targetUserId = targetUserId;
            }

            this.ws.send(JSON.stringify(payload));
            console.log('Sent chat message:', message);
        } else {
            console.warn('Chat WebSocket not connected');
        }
    }

    send(data) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(data));
        }
    }

    addListener(id, callback) {
        this.listeners.set(id, callback);
    }

    removeListener(id) {
        this.listeners.delete(id);
    }

    notifyListeners(type, data) {
        this.listeners.forEach((callback) => {
            try {
                callback(type, data);
            } catch (error) {
                console.error('Error in chat listener:', error);
            }
        });
    }

    disconnect() {
        if (this.ws) {
            this.ws.close(1000, 'Client disconnect');
            this.ws = null;
        }
        this.listeners.clear();
    }

    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }
}

export default new ChatService();