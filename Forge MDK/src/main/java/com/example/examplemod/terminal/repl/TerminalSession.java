package com.example.examplemod.terminal.repl;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds terminal state independent from rendering.
 */
public class TerminalSession {
    private static final int MAX_HISTORY = 200;

    private final List<String> history = new ArrayList<>();
    private final TerminalCommandProcessor commandProcessor;
    private final String prompt;

    public TerminalSession(TerminalCommandProcessor commandProcessor, String prompt) {
        this.commandProcessor = commandProcessor;
        this.prompt = prompt;
        addOutput("REALITY RESTORATION TERMINAL v0.1");
        addOutput("type 'help' for commands");
    }

    public List<String> getHistory() {
        return List.copyOf(history);
    }

    public boolean submit(String input) {
        String normalized = input == null ? "" : input.trim();
        addOutput(prompt + normalized);

        if (normalized.isEmpty()) {
            return false;
        }

        String commandKey = normalized.toLowerCase();
        if ("help".equals(commandKey)) {
            addOutput("commands: help, clear, exit, <any text>");
            return false;
        }
        if ("clear".equals(commandKey)) {
            history.clear();
            addOutput("terminal buffer cleared");
            return false;
        }
        if ("exit".equals(commandKey)) {
            addOutput("closing terminal...");
            return true;
        }

        for (String line : commandProcessor.process(normalized)) {
            addOutput(line);
        }
        return false;
    }

    private void addOutput(String line) {
        history.add(line);
        if (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }
}
