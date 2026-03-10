package com.example.examplemod.terminal.command;

import java.util.List;

public record TerminalCommandResult(
        boolean handled,
        boolean closeScreen,
        boolean startWorkflow,
        List<String> outputLines
) {
    public static TerminalCommandResult notHandled() {
        return new TerminalCommandResult(false, false, false, List.of());
    }

    public static TerminalCommandResult handled(List<String> outputLines) {
        return new TerminalCommandResult(true, false, false, outputLines);
    }

    public static TerminalCommandResult close(List<String> outputLines) {
        return new TerminalCommandResult(true, true, false, outputLines);
    }
}
