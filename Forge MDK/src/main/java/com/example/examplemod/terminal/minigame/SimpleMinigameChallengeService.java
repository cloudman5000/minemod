package com.example.examplemod.terminal.minigame;

public final class SimpleMinigameChallengeService implements MinigameChallengeService {
    @Override
    public boolean shouldRequireChallenge(String command) {
        String normalized = command.toLowerCase();
        return normalized.contains("admin")
                || normalized.contains("root")
                || normalized.contains("fortress")
                || normalized.contains("ultimate");
    }
}
