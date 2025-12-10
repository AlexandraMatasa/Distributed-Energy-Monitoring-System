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
                if (selectedSession && message.userId === selectedSession.userId) {
                    setConversationHistory(prev => [...prev, message]);
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
        const date = new Date(timestamp);
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
    };

    const getLastMessage = (session) => {
        if (!session.conversationHistory || session.conversationHistory.length === 0) {
            return 'No messages yet';
        }
        const lastMsg = session.conversationHistory[session.conversationHistory.length - 1];
        return lastMsg.message.substring(0, 50) + (lastMsg.message.length > 50 ? '...' : '');
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
                                        <small className="text-muted ms-2">
                                            {new Date(msg.timestamp).toLocaleTimeString()}
                                        </small>
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