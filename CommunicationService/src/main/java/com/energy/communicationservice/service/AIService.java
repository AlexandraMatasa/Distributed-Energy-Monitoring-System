package com.energy.communicationservice.service;

import com.energy.communicationservice.dto.AIRequestDTO;
import com.energy.communicationservice.dto.AIResponseDTO;
import com.energy.communicationservice.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private final WebClient webClient;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${groq.temperature:0.7}")
    private double temperature;

    @Value("${groq.max-tokens:500}")
    private int maxTokens;

    public AIService(@Value("${groq.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1/chat/completions")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("AIService initialized with Groq API - Model: llama-3.3-70b-versatile");
    }

    public String generateResponse(String userMessage, String username) {
        try {
            log.info("Sending request to Groq API for user: {}", username);

            AIRequestDTO requestBody = new AIRequestDTO(
                    "llama-3.3-70b-versatile",
                    List.of(
                            new ChatMessage("system",
                                    "You are a helpful energy support assistant for an Energy Management System. " +
                                            "Provide concise, friendly, and accurate responses. " +
                                            "If you don't know something, suggest contacting an administrator. " +
                                            "Keep answers short and under 150 words."),
                            new ChatMessage("user", userMessage)
                    )
            );

            requestBody.setTemperature(temperature);
            requestBody.setMax_tokens(maxTokens);

            AIResponseDTO response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(AIResponseDTO.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null &&
                    response.getChoices() != null &&
                    !response.getChoices().isEmpty()) {

                String aiResponse = response.getChoices().get(0).getMessage().getContent();

                log.info("Received AI response (length: {} chars, tokens: {})",
                        aiResponse.length(),
                        response.getUsage() != null ? response.getUsage().getTotal_tokens() : "N/A");

                return aiResponse;
            } else {
                log.warn("Empty response from Groq API");
                return "I'm having trouble generating a response right now. Please try again or contact an administrator.";
            }

        } catch (Exception e) {
            log.error("Error calling Groq API: {}", e.getMessage(), e);
            return "I'm temporarily unavailable. Please contact an administrator for assistance.";
        }
    }

    public boolean isAvailable() {
        try {
            AIRequestDTO testRequest = new AIRequestDTO(
                    model,
                    List.of(new ChatMessage("user", "Hi"))
            );
            testRequest.setMax_tokens(10);

            webClient.post()
                    .bodyValue(testRequest)
                    .retrieve()
                    .bodyToMono(AIResponseDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("Groq API health check: OK");
            return true;
        } catch (Exception e) {
            log.warn("Groq API health check failed: {}", e.getMessage());
            return false;
        }
    }
}