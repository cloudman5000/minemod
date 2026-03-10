package com.example.examplemod.terminal.config;

import java.util.List;

public record TerminalRewardDefinition(
        String id,
        String title,
        String description,
        int corruptionCost,
        int cooldownSeconds,
        List<String> commands,
        String requiredFlag
) {
}
