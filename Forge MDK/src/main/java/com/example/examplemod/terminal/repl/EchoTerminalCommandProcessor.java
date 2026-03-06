package com.example.examplemod.terminal.repl;

import java.util.List;

/**
 * Initial terminal behavior: echo input so gameplay and UI can be tested quickly.
 */
public class EchoTerminalCommandProcessor implements TerminalCommandProcessor {
    @Override
    public List<String> process(String command) {
        return List.of("echo: " + command);
    }
}
