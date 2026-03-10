package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.domain.TerminalState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalRuntimeEngineArcadeControlTest {
    @Test
    void detectsArcadeControlPrefix() {
        assertTrue(TerminalRuntimeEngine.isArcadeControlCommand("__arcade move -1"));
        assertTrue(TerminalRuntimeEngine.isArcadeControlCommand("   __arcade fire"));
        assertFalse(TerminalRuntimeEngine.isArcadeControlCommand("open pong"));
    }

    @Test
    void ignoresArcadeControlsWhenNoActiveMinigame() {
        TerminalRuntimeEngine runtime = new TerminalRuntimeEngine();
        TerminalState state = new TerminalState();
        state.setArcadeMoveIntent(0);

        boolean consumed = runtime.tryConsumeArcadeControl(state, "__arcade move 1");

        assertTrue(consumed);
        assertEquals(0, state.arcadeMoveIntent());
    }

    @Test
    void appliesArcadeControlsWhenMinigameIsActive() {
        TerminalRuntimeEngine runtime = new TerminalRuntimeEngine();
        TerminalState state = new TerminalState();
        state.setActiveMinigameId("pong84");

        boolean moveConsumed = runtime.tryConsumeArcadeControl(state, "__arcade move -1");
        boolean fireConsumed = runtime.tryConsumeArcadeControl(state, "__arcade fire");

        assertTrue(moveConsumed);
        assertTrue(fireConsumed);
        assertEquals(-1, state.arcadeMoveIntent());
        assertTrue(state.arcadeFireQueued());
    }
}
