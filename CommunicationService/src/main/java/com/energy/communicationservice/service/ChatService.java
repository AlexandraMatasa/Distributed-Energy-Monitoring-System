package com.energy.communicationservice.service;

import com.energy.communicationservice.dto.ChatMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRuleService ruleService;
    private final AIService aiService;
    private final ChatSessionManager sessionManager;

    @Value("${chat.rules.enabled:true}")
    private boolean rulesEnabled;

    @Value("${chat.ai.enabled:true}")
    private boolean aiEnabled;

    @Autowired
    public ChatService(ChatRuleService ruleService, AIService aiService, ChatSessionManager sessionManager) {
        this.ruleService = ruleService;
        this.aiService = aiService;
        this.sessionManager = sessionManager;
    }

    public ChatMessageDTO processUserMessage(ChatMessageDTO userMessage) {
        log.info("Processing message from user: {}", userMessage.getUserId());

        if (sessionManager.isHumanHandoffActive(userMessage.getUserId())) {
            log.info("User {} is in human handoff mode - forwarding to admin", userMessage.getUserId());
            sessionManager.addMessageToSession(userMessage.getUserId(), userMessage);
            return null;
        }

        if (rulesEnabled && ruleService.isHumanHandoffRequested(userMessage.getMessage())) {
            log.info("Human handoff REQUESTED by user: {}", userMessage.getUserId());
            sessionManager.enableHumanHandoff(userMessage.getUserId());
            sessionManager.addMessageToSession(userMessage.getUserId(), userMessage);

            ChatMessageDTO handoffMessage = new ChatMessageDTO(
                    null,
                    "Support Bot",
                    "BOT",
                    "I'm connecting you with a live support agent. Please wait a moment...",
                    userMessage.getSessionId()
            );
            sessionManager.addMessageToSession(userMessage.getUserId(), handoffMessage);

            return handoffMessage;
        }

        if (rulesEnabled) {
            Optional<String> ruleResponse = ruleService.matchRule(userMessage.getMessage());
            if (ruleResponse.isPresent()) {
                log.info("Rule matched for message");
                ChatMessageDTO response = new ChatMessageDTO(
                        null,
                        "Support Bot",
                        "BOT",
                        ruleResponse.get(),
                        userMessage.getSessionId()
                );

                sessionManager.getOrCreateSession(
                        userMessage.getUserId(),
                        userMessage.getUsername(),
                        userMessage.getSessionId()
                );
                sessionManager.addMessageToSession(userMessage.getUserId(), userMessage);
                sessionManager.addMessageToSession(userMessage.getUserId(), response);

                return response;
            }
        }

        if (aiEnabled) {
            log.info("No rule matched, delegating to AI");
            String responseText = aiService.generateResponse(
                    userMessage.getMessage(),
                    userMessage.getUsername()
            );

            ChatMessageDTO response = new ChatMessageDTO(
                    null,
                    "AI Assistant",
                    "BOT",
                    responseText,
                    userMessage.getSessionId()
            );

            sessionManager.getOrCreateSession(
                    userMessage.getUserId(),
                    userMessage.getUsername(),
                    userMessage.getSessionId()
            );
            sessionManager.addMessageToSession(userMessage.getUserId(), userMessage);
            sessionManager.addMessageToSession(userMessage.getUserId(), response);

            return response;
        }

        log.info("Rules and AI disabled, forwarding to admin");
        sessionManager.getOrCreateSession(
                userMessage.getUserId(),
                userMessage.getUsername(),
                userMessage.getSessionId()
        );
        sessionManager.addMessageToSession(userMessage.getUserId(), userMessage);
        return null;
    }
}