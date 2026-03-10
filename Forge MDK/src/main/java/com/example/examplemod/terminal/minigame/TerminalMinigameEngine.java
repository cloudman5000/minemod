package com.example.examplemod.terminal.minigame;

public interface TerminalMinigameEngine {
    String kind();

    MinigameTurnResult start(int targetScore);

    MinigameTurnResult step(MinigameInput input, String serializedState, int targetScore);

    default MinigameTurnResult handleInput(String input, String serializedState, int targetScore) {
        String normalized = input == null ? "" : input.trim().toLowerCase();
        int move = 0;
        boolean fire = false;
        if (normalized.contains("left")) {
            move = -1;
        } else if (normalized.contains("right")) {
            move = 1;
        }
        if (normalized.contains("shoot") || normalized.contains("fire")) {
            fire = true;
        }
        return step(MinigameInput.of(move, fire), serializedState, targetScore);
    }
}
