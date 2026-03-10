package com.example.examplemod.terminal.config;

import java.util.List;

public record TerminalScriptDefinition(
        String id,
        String title,
        List<String> lines,
        String grantsFlag
) {
}
