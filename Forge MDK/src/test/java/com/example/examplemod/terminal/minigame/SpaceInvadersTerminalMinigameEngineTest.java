package com.example.examplemod.terminal.minigame;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceInvadersTerminalMinigameEngineTest {
    @Test
    void invadersReturnsStateAndAdvancesWithFireInputs() {
        SpaceInvadersTerminalMinigameEngine engine = new SpaceInvadersTerminalMinigameEngine();
        MinigameTurnResult result = engine.start(10);
        assertFalse(result.completed());
        assertTrue(result.outputLines().stream().anyMatch(line -> line.contains("SPACE_PANIC")));

        for (int i = 0; i < 20 && !result.completed() && !result.failed(); i++) {
            result = engine.handleInput("shoot", result.stateJson(), 10);
        }
        assertTrue(result.stateJson() != null && !result.stateJson().isBlank());
        assertTrue(result.score() >= 0);
    }
}
