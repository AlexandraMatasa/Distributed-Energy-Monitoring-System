import React, { useState, useEffect, useRef } from 'react';
import { Card, CardHeader, CardBody, Input, Button, Badge } from 'reactstrap';
import ChatService from '../services/ChatService';
import './ChatWidget.css';

function ChatWidget() {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const [unreadCount, setUnreadCount] = useState(0);
    const [isConnected, setIsConnected] = useState(false);

    const messagesEndRef = useRef(null);
    const userId = localStorage.getItem('userId');
    const username = localStorage.getItem('username');
    const role = localStorage.getItem('role');

    useEffect(() => {
        if (role !== 'CLIENT') return;

        const handleChatMessage = (type, data) => {
            if (type === 'chat_message') {
                setMessages(prev => [...prev, data.data]);
                if (!isOpen) {
                    setUnreadCount(prev => prev + 1);
                }
            } else if (type === 'registered') {
                setIsConnected(true);
            }
        };

        ChatService.addListener('chat-widget', handleChatMessage);
        ChatService.connect(userId, username, role);

        return () => {
            ChatService.removeListener('chat-widget');
        };
    }, [userId, username, role]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const toggleChat = () => {
        setIsOpen(!isOpen);
        if (!isOpen) {
            setUnreadCount(0);
        }
    };

    const handleSendMessage = () => {
        if (inputMessage.trim()) {
            ChatService.sendMessage(inputMessage.trim());
            setInputMessage('');
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const formatMessageTime = (timestamp) => {
        if (!timestamp) return '';

        try {
            const date = new Date(timestamp);
            if (isNaN(date.getTime())) {
                console.warn('Invalid timestamp:', timestamp);
                return '';
            }
            return date.toLocaleTimeString();
        } catch (error) {
            console.error('Error formatting timestamp:', error, timestamp);
            return '';
        }
    };

    if (role !== 'CLIENT') return null;

    return (
        <>
            {!isOpen && (
                <div className="chat-button" onClick={toggleChat}>
                    {unreadCount > 0 && (
                        <Badge color="danger" className="chat-badge">
                            {unreadCount}
                        </Badge>
                    )}
                </div>
            )}

            {isOpen && (
                <Card className="chat-widget">
                    <CardHeader className="chat-header">
                        <div className="d-flex justify-content-between align-items-center">
                            <div>
                                <strong>Customer Support</strong>
                                {isConnected && (
                                    <Badge color="success" className="ms-2" pill>Online</Badge>
                                )}
                            </div>
                            <Button
                                close
                                onClick={toggleChat}
                                style={{ color: 'white' }}
                            />
                        </div>
                    </CardHeader>
                    <CardBody className="chat-body">
                        <div className="messages-container">
                            {messages.length === 0 && (
                                <div className="text-center text-muted py-3">
                                    <p>Welcome to customer support!</p>
                                    <p className="small">Ask me anything about the system.</p>
                                </div>
                            )}
                            {messages.map((msg, index) => (
                                <div
                                    key={index}
                                    className={`message ${
                                        msg.role === 'CLIENT' ? 'message-sent' : 'message-received'
                                    }`}
                                >
                                    <div className="message-header">
                                        <strong>{msg.username || msg.role}</strong>
                                        <small className="text-muted ms-2">
                                            {formatMessageTime(msg.timestamp)}
                                        </small>
                                    </div>
                                    <div className="message-content">
                                        {msg.message}
                                    </div>
                                </div>
                            ))}
                            <div ref={messagesEndRef} />
                        </div>
                        <div className="message-input">
                            <Input
                                type="textarea"
                                value={inputMessage}
                                onChange={(e) => setInputMessage(e.target.value)}
                                onKeyPress={handleKeyPress}
                                placeholder="Type your message..."
                                rows={2}
                                disabled={!isConnected}
                            />
                            <Button
                                color="primary"
                                onClick={handleSendMessage}
                                disabled={!inputMessage.trim() || !isConnected}
                                block
                                className="mt-2"
                            >
                                Send
                            </Button>
                        </div>
                    </CardBody>
                </Card>
            )}
        </>
    );
}

export default ChatWidget;