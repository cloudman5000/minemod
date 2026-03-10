package com.example.examplemod.terminal.ai;

public final class DefaultAiPromptComposer implements AiPromptComposer {
    @Override
    public String compose(String command, String terminalContext, String commandHistorySummary) {
        return "Command: " + command + "\nContext: " + terminalContext + "\nRecent history: " + commandHistorySummary
                + "\nCompose a creative immersive terminal sequence with directives (MC/WAIT/PROGRESS/SPINNER/TREE/LIVE/TTS) when useful.";
    }
}
