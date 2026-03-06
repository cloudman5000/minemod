package com.example.examplemod.terminal.repl;

import java.util.List;

/**
 * Parses terminal commands and returns output lines.
 */
public interface TerminalCommandProcessor {
    List<String> process(String command);
}
