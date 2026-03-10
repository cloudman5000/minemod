package com.example.examplemod.terminal.intent;

import java.util.Set;

public final class TerminalIntentRouter {
    private static final Set<String> HARD_CODED_COMMANDS = Set.of(
            "help", "exit", "status", "terminals", "link", "send",
            "broadcast", "inbox", "history", "connect", "workflows", "cancel"
    );

    private static final Set<String> HACKING_KEYWORDS = Set.of(
            "scan", "analyze", "hack", "exploit", "override", "inject", "decrypt",
            "trace", "bypass", "escalate", "ping", "probe", "optimize", "enhance",
            "patch", "netstat", "ls", "dir", "cat", "whoami"
    );

    public IntentClassificationResult classify(String rawInput) {
        String normalized = rawInput == null ? "" : rawInput.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return new IntentClassificationResult(TerminalIntentType.UNKNOWN, "empty_input");
        }

        String firstToken = normalized.split("\\s+")[0];
        if (HARD_CODED_COMMANDS.contains(firstToken)) {
            if ("cancel".equals(firstToken) || "workflows".equals(firstToken)) {
                return new IntentClassificationResult(TerminalIntentType.WORKFLOW_CONTROL, "workflow_control_keyword");
            }
            return new IntentClassificationResult(TerminalIntentType.HARDCODED_COMMAND, "hardcoded_keyword");
        }

        if (HACKING_KEYWORDS.stream().anyMatch(normalized::contains)) {
            return new IntentClassificationResult(TerminalIntentType.HACKING_INTENT, "contains_hacking_keyword");
        }

        if (normalized.contains(">>") || normalized.contains("|") || normalized.contains("--")) {
            return new IntentClassificationResult(TerminalIntentType.HACKING_INTENT, "shell_like_pattern");
        }

        return new IntentClassificationResult(TerminalIntentType.HACKING_INTENT, "default_to_hacking_intent");
    }
}
