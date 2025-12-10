import React, { useState, useEffect, useRef } from 'react';
import { Card, Input, Button, Badge } from 'reactstrap';
import ChatService from '../monitoring/services/ChatService';
import './AdminChatPanel.css';

function AdminChatPanel() {
    const [sessions, setSessions] = useState([]);
    const [selectedSession, setSelectedSession] = useState(null);
    const [conversationHistory, setConversationHistory] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);

    const messagesEndRef = useRef(null);
    const userId = localStorage.getItem('userId');
    const username = localStorage.getItem('username');
    const role = localStorage.getItem('role');

    useEffect(() => {
        if (role !== 'ADMIN') {
            window.location.href = '/access-denied';
            return;
        }

        const handleChatMessage = (type, data) => {
            console.log('Admin received message:', type, data);

            if (type === 'registered') {
                setIsConnected(true);
                ChatService.send({ action: 'get_sessions' });
            } else if (type === 'sessions_list') {
                setSessions(data.data || []);
                console.log('Updated sessions list:', data.data);
            } else if (type === 'chat_message') {
                const message = data.data;

                if (message.userId === userId && message.role === 'ADMIN') {
                    console.log('Ignoring own message (already in conversation from optimistic update)');
                    ChatService.send({ action: 'get_sessions' });
                    return;
                }

                if (selectedSession && message.userId === selectedSession.userId) {
                    setConversationHistory(prev => {
                        const messageExists = prev.some(m =>
                            m.message === message.message &&
                            m.role === message.role &&
                            Math.abs(new Date(m.timestamp) - new Date(message.timestamp)) < 2000 // within 2 seconds
                        );

                        if (messageExists) {
                            console.log('Duplicate message detected, skipping');
                            return prev;
                        }

                        return [...prev, message];
                    });
                }

                ChatService.send({ action: 'get_sessions' });
            } else if (type === 'conversation_history') {
                const session = data.data;
                setConversationHistory(session.conversationHistory || []);

                if (session.userId) {
                    ChatService.send({
                        action: 'mark_read',
                        userId: session.userId
                    });
                }
            }
        };

        ChatService.addListener('admin-chat-panel', handleChatMessage);
        ChatService.connect(userId, username, role);

        return () => {
            ChatService.removeListener('admin-chat-panel');
        };
    }, [userId, username, role, selectedSession]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [conversationHistory]);

    const handleSessionClick = (session) => {
        console.log('Selected session:', session);
        setSelectedSession(session);

        ChatService.send({
            action: 'get_conversation',
            userId: session.userId
        });
    };

    const handleSendMessage = () => {
        if (inputMessage.trim() && selectedSession) {
            const optimisticMessage = {
                userId: userId,
                username: username,
                role: 'ADMIN',
                message: inputMessage.trim(),
                timestamp: new Date().toISOString(),
                sessionId: selectedSession.sessionId
            };

            setConversationHistory(prev => [...prev, optimisticMessage]);

            ChatService.sendMessage(inputMessage.trim(), selectedSession.userId);
            setInputMessage('');
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const formatTime = (timestamp) => {
        if (!timestamp) return 'Just now';

        try {
            // Try parsing as ISO string first
            let date = new Date(timestamp);

            // If invalid, try parsing as array format [year, month, day, hour, minute, second, nano]
            if (isNaN(date.getTime()) && Array.isArray(timestamp)) {
                date = new Date(
                    timestamp[0],
                    timestamp[1] - 1,
                    timestamp[2],
                    timestamp[3] || 0,
                    timestamp[4] || 0,
                    timestamp[5] || 0
                );
            }

            if (isNaN(date.getTime())) {
                return 'Just now';
            }

            const now = new Date();
            const diffMs = now - date;
            const diffMins = Math.floor(diffMs / 60000);
            const diffHours = Math.floor(diffMs / 3600000);
            const diffDays = Math.floor(diffMs / 86400000);

            if (diffMins < 1) return 'Just now';
            if (diffMins < 60) return `${diffMins}m ago`;
            if (diffHours < 24) return `${diffHours}h ago`;
            if (diffDays < 7) return `${diffDays}d ago`;
            return date.toLocaleDateString();
        } catch (error) {
            console.error('Error formatting time:', error, timestamp);
            return 'Just now';
        }
    };

    const getLastMessage = (session) => {
        if (!session.conversationHistory || session.conversationHistory.length === 0) {
            return 'No messages yet';
        }
        const lastMsg = session.conversationHistory[session.conversationHistory.length - 1];
        return lastMsg.message.substring(0, 50) + (lastMsg.message.length > 50 ? '...' : '');
    };

    const formatMessageTime = (timestamp) => {
        if (!timestamp) return '';

        try {
            const date = new Date(timestamp);
            if (isNaN(date.getTime())) {
                return '';
            }
            return date.toLocaleTimeString();
        } catch (error) {
            console.error('Error formatting message time:', error, timestamp);
            return '';
        }
    };

    return (
        <div className="admin-chat-container">
            <div className="sessions-sidebar">
                <div className="sessions-header">
                    <h4>
                        Active Chats
                        {isConnected && (
                            <Badge color="success" pill className="ms-2">Online</Badge>
                        )}
                    </h4>
                    <small>{sessions.length} conversation{sessions.length !== 1 ? 's' : ''}</small>
                </div>
                <div className="sessions-list">
                    {sessions.length === 0 ? (
                        <div className="no-sessions-message">
                            <p>No active chat sessions</p>
                            <small className="text-muted">
                                Clients requesting human support will appear here
                            </small>
                        </div>
                    ) : (
                        sessions.map((session, index) => (
                            <div
                                key={index}
                                className={`session-item ${
                                    selectedSession?.userId === session.userId ? 'active' : ''
                                } ${session.unreadAdminCount > 0 ? 'has-unread' : ''}`}
                                onClick={() => handleSessionClick(session)}
                            >
                                <div className="session-username">
                                    {session.username || 'Unknown User'}
                                </div>
                                <div className="session-preview">
                                    {getLastMessage(session)}
                                </div>
                                <div className="session-time">
                                    {formatTime(session.lastMessageTime)}
                                </div>
                                {session.unreadAdminCount > 0 && (
                                    <div className="session-unread-badge">
                                        {session.unreadAdminCount}
                                    </div>
                                )}
                            </div>
                        ))
                    )}
                </div>
            </div>

            <div className="conversation-panel">
                {selectedSession ? (
                    <>
                        <div className="conversation-header">
                            <h5>
                                {selectedSession.username || 'Unknown User'}
                            </h5>
                            <div className="text-muted">
                                <small>User ID: {selectedSession.userId}</small>
                            </div>
                        </div>

                        <div className="conversation-messages">
                            {selectedSession.humanHandoffRequested && (
                                <div className="handoff-indicator">
                                    <span>
                                        <strong>Human support requested</strong> -
                                        Client is now chatting directly with you
                                    </span>
                                </div>
                            )}

                            {conversationHistory.map((msg, index) => (
                                <div
                                    key={index}
                                    className={`admin-message-item ${
                                        msg.role === 'ADMIN' ? 'admin-message-sent' :
                                            msg.role === 'CLIENT' ? 'admin-message-received' :
                                                'admin-message-bot'
                                    }`}
                                >
                                    <div className="admin-message-header">
                                        <strong>
                                            {msg.role === 'ADMIN' ? 'You' : msg.username || msg.role}
                                        </strong>
                                    </div>
                                    <div className="admin-message-content">
                                        {msg.message}
                                    </div>
                                </div>
                            ))}
                            <div ref={messagesEndRef} />
                        </div>

                        <div className="conversation-input-area">
                            <Input
                                type="textarea"
                                value={inputMessage}
                                onChange={(e) => setInputMessage(e.target.value)}
                                onKeyPress={handleKeyPress}
                                placeholder="Type your reply..."
                                rows={3}
                                disabled={!isConnected}
                            />
                            <Button
                                color="primary"
                                onClick={handleSendMessage}
                                disabled={!inputMessage.trim() || !isConnected}
                                block
                                className="mt-2"
                            >
                                Send Reply
                            </Button>
                        </div>
                    </>
                ) : (
                    <div className="empty-conversation">
                        <h5>No Conversation Selected</h5>
                        <p className="text-muted">
                            Select a client from the sidebar to view their conversation
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
}

export default AdminChatPanel;