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

    @Value("${chat.rules.enabled:true}")
    private boolean rulesEnabled;

    @Value("${chat.ai.enabled:true}")
    private boolean aiEnabled;

    @Autowired
    public ChatService(ChatRuleService ruleService, AIService aiService) {
        this.ruleService = ruleService;
        this.aiService = aiService;
    }

    public ChatMessageDTO processUserMessage(ChatMessageDTO userMessage) {
        log.info("Processing message from user: {}", userMessage.getUserId());

        if (rulesEnabled) {
            Optional<String> ruleResponse = ruleService.matchRule(userMessage.getMessage());
            if (ruleResponse.isPresent()) {
                log.info("Rule matched for message");
                return new ChatMessageDTO(
                        null,
                        "Support Bot",
                        "BOT",
                        ruleResponse.get(),
                        userMessage.getSessionId()
                );
            }
        }

        if (aiEnabled) {
            log.info("No rule matched, delegating to AI");
            String responseText = aiService.generateResponse(
                    userMessage.getMessage(),
                    userMessage.getUsername()
            );
            return new ChatMessageDTO(
                    null,
                    "AI Assistant",
                    "BOT",
                    responseText,
                    userMessage.getSessionId()
            );
        }

        log.info("Rules and AI disabled, message will be forwarded to admin");
        return null;
    }
}