package com.example.examplemod.terminal.minigame;

import java.util.List;

public record MinigameTurnResult(
        String stateJson,
        int score,
        boolean completed,
        boolean failed,
        List<String> outputLines
) {
}
