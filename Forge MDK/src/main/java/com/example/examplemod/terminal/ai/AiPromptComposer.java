package com.example.examplemod.terminal.ai;

public interface AiPromptComposer {
    String compose(String command, String terminalContext, String commandHistorySummary);
}
