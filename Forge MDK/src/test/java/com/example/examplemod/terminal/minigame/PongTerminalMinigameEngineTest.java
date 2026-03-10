package com.example.examplemod.terminal.minigame;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PongTerminalMinigameEngineTest {
    @Test
    void pongProgressesAndEventuallyCompletes() {
        PongTerminalMinigameEngine engine = new PongTerminalMinigameEngine();
        MinigameTurnResult result = engine.start(6);
        assertFalse(result.completed());
        assertTrue(result.outputLines().stream().anyMatch(line -> line.contains("PONG_84")));

        for (int i = 0; i < 400 && !result.completed() && !result.failed(); i++) {
            result = engine.handleInput("stay", result.stateJson(), 6);
        }

        assertTrue(result.completed() || result.failed());
        assertTrue(result.stateJson() != null && !result.stateJson().isBlank());
    }
}
