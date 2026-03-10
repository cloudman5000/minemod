package com.example.examplemod.terminal.config;

import java.util.List;

public record TerminalCorruptionConfig(
        List<Integer> stageThresholds,
        int ambientSpawnIntervalTicks,
        int maxCorruption
) {
}
