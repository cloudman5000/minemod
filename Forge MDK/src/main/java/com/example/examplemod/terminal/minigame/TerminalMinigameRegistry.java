package com.example.examplemod.terminal.minigame;

import java.util.HashMap;
import java.util.Map;

public final class TerminalMinigameRegistry {
    private final Map<String, TerminalMinigameEngine> byKind = new HashMap<>();

    public TerminalMinigameRegistry() {
        register(new PongTerminalMinigameEngine());
        register(new SpaceInvadersTerminalMinigameEngine());
    }

    public void register(TerminalMinigameEngine engine) {
        byKind.put(engine.kind().toLowerCase(), engine);
    }

    public TerminalMinigameEngine resolve(String kind) {
        if (kind == null || kind.isBlank()) {
            return byKind.get("pong");
        }
        return byKind.getOrDefault(kind.toLowerCase(), byKind.get("pong"));
    }
}
