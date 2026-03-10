package com.example.examplemod.terminal.ai;

public record TerminalAiConfig(
        String apiKey,
        String model,
        String endpoint,
        int timeoutSeconds
) {
    public static TerminalAiConfig defaults() {
        return new TerminalAiConfig("", "gpt-5.3-mini", "https://api.openai.com/v1/chat/completions", 20);
    }
}
