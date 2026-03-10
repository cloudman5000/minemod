package com.example.examplemod.terminal.intent;

public record IntentClassificationResult(
        TerminalIntentType type,
        String why
) {
}
