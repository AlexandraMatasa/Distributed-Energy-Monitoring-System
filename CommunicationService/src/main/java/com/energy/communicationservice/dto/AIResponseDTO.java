package com.energy.communicationservice.dto;

import java.util.List;

public class AIResponseDTO {
    private List<CompletionChoice> choices;
    private TokenUsage usage;

    public static class CompletionChoice {
        private ChatMessage message;
        private String finish_reason;

        public ChatMessage getMessage() { return message; }
        public void setMessage(ChatMessage message) { this.message = message; }

        public String getFinish_reason() { return finish_reason; }
        public void setFinish_reason(String finish_reason) { this.finish_reason = finish_reason; }
    }

    public static class TokenUsage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;

        public int getPrompt_tokens() { return prompt_tokens; }
        public void setPrompt_tokens(int prompt_tokens) { this.prompt_tokens = prompt_tokens; }

        public int getCompletion_tokens() { return completion_tokens; }
        public void setCompletion_tokens(int completion_tokens) { this.completion_tokens = completion_tokens; }

        public int getTotal_tokens() { return total_tokens; }
        public void setTotal_tokens(int total_tokens) { this.total_tokens = total_tokens; }
    }

    public List<CompletionChoice> getChoices() { return choices; }
    public void setChoices(List<CompletionChoice> choices) { this.choices = choices; }

    public TokenUsage getUsage() { return usage; }
    public void setUsage(TokenUsage usage) { this.usage = usage; }
}
