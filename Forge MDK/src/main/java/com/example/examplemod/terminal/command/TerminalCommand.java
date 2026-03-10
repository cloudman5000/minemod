package com.example.examplemod.terminal.command;

import java.util.List;

public interface TerminalCommand {
    TerminalCommandResult execute(TerminalCommandContext context, List<String> args);
}
