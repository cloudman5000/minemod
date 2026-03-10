package com.example.examplemod.terminal.config;

import java.util.Map;

public record TerminalGameplayConfig(
        boolean aiFlavorEnabled,
        String rootNodeId,
        Map<String, TerminalMenuNode> nodes,
        Map<String, TerminalScriptDefinition> apps,
        Map<String, TerminalRewardDefinition> rewards,
        Map<String, TerminalMinigameDefinition> minigames,
        TerminalCorruptionConfig corruption
) {
}
