package com.example.examplemod.terminal.minigame;

public record MinigameInput(int move, boolean fire) {
    public static final MinigameInput IDLE = new MinigameInput(0, false);

    public static MinigameInput of(int move, boolean fire) {
        int clamped = Math.max(-1, Math.min(1, move));
        return new MinigameInput(clamped, fire);
    }
}
