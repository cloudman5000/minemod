package com.example.examplemod.terminal.config;

public record TerminalMinigameDefinition(
        String id,
        String title,
        String kind,
        int targetScore,
        int corruptionCost,
        String rewardId,
        String grantsFlag
) {
}
