package com.example.examplemod.terminal.config;

public record TerminalMenuEntry(
        String id,
        String label,
        TerminalMenuEntryType type,
        String target,
        int corruptionCost,
        String requiredFlag,
        String grantsFlag
) {
}
