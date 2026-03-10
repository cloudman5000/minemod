package com.example.examplemod.terminal.minigame;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public final class SpaceInvadersTerminalMinigameEngine implements TerminalMinigameEngine {
    private static final Gson GSON = new Gson();
    private static final int WIDTH = 13;
    private static final int HEIGHT = 11;

    @Override
    public String kind() {
        return "invaders";
    }

    @Override
    public MinigameTurnResult start(int targetScore) {
        InvaderState state = new InvaderState();
        state.tick = 0;
        state.shipX = WIDTH / 2;
        state.lives = 3;
        state.direction = 1;
        state.stepCounter = 0;
        state.score = 0;
        state.completed = false;
        state.invaders = defaultInvaders();
        state.shots = new ArrayList<>();
        return new MinigameTurnResult(
                GSON.toJson(state),
                state.score,
                false,
                false,
                List.of("[game] SPACE_PANIC live controls: A/D move, SPACE fire")
        );
    }

    @Override
    public MinigameTurnResult step(MinigameInput input, String serializedState, int targetScore) {
        InvaderState state = deserialize(serializedState);
        state.shipX = Math.max(0, Math.min(WIDTH - 1, state.shipX + input.move()));
        if (input.fire() && state.shots.size() < 3) {
            state.shots.add(new Projectile(state.shipX, HEIGHT - 2));
        }

        List<Projectile> nextShots = new ArrayList<>();
        for (Projectile shot : state.shots) {
            int ny = shot.y - 1;
            if (ny < 0) {
                continue;
            }
            int hitIndex = findInvaderAt(state.invaders, shot.x, ny);
            if (hitIndex >= 0) {
                state.invaders.remove(hitIndex);
                state.score += 3;
            } else {
                nextShots.add(new Projectile(shot.x, ny));
            }
        }
        state.shots = nextShots;

        if (!state.invaders.isEmpty()) {
            state.stepCounter++;
            int cadence = state.invaders.size() <= 6 ? 8 : 12;
            if (state.stepCounter >= cadence) {
                state.stepCounter = 0;
                advanceInvaders(state);
            }
        }

        int frontline = lowestInvaderRow(state.invaders);
        if (frontline >= HEIGHT - 2 && !state.invaders.isEmpty()) {
            state.lives -= 1;
            state.invaders = defaultInvaders();
            state.shots = new ArrayList<>();
            state.direction = 1;
            state.stepCounter = 0;
        }

        state.tick++;
        boolean completed = state.score >= targetScore || state.invaders.isEmpty();
        boolean failed = state.lives <= 0;
        state.completed = completed;
        return new MinigameTurnResult(GSON.toJson(state), state.score, completed, failed, List.of());
    }

    private InvaderState deserialize(String raw) {
        try {
            InvaderState state = GSON.fromJson(raw, InvaderState.class);
            if (state != null && state.invaders != null && state.shots != null) {
                return state;
            }
        } catch (Exception ignored) {
        }
        InvaderState fallback = new InvaderState();
        fallback.tick = 0;
        fallback.shipX = WIDTH / 2;
        fallback.lives = 3;
        fallback.direction = 1;
        fallback.stepCounter = 0;
        fallback.invaders = defaultInvaders();
        fallback.shots = new ArrayList<>();
        return fallback;
    }

    private void advanceInvaders(InvaderState state) {
        boolean wouldHitLeft = state.invaders.stream().anyMatch(i -> i.x <= 0 && state.direction < 0);
        boolean wouldHitRight = state.invaders.stream().anyMatch(i -> i.x >= WIDTH - 1 && state.direction > 0);
        if (wouldHitLeft || wouldHitRight) {
            for (Invader invader : state.invaders) {
                invader.y += 1;
            }
            state.direction *= -1;
            return;
        }
        for (Invader invader : state.invaders) {
            invader.x += state.direction;
        }
    }

    private int findInvaderAt(List<Invader> invaders, int x, int y) {
        for (int i = 0; i < invaders.size(); i++) {
            Invader invader = invaders.get(i);
            if (invader.x == x && invader.y == y) {
                return i;
            }
        }
        return -1;
    }

    private int lowestInvaderRow(List<Invader> invaders) {
        int max = -1;
        for (Invader invader : invaders) {
            if (invader.y > max) {
                max = invader.y;
            }
        }
        return max;
    }

    private List<Invader> defaultInvaders() {
        List<Invader> invaders = new ArrayList<>();
        for (int y = 1; y <= 3; y++) {
            for (int x = 2; x <= 10; x += 2) {
                invaders.add(new Invader(x, y));
            }
        }
        return invaders;
    }

    private static final class InvaderState {
        int tick;
        int shipX;
        int lives;
        int score;
        int direction;
        int stepCounter;
        boolean completed;
        List<Invader> invaders;
        List<Projectile> shots;
    }

    private static final class Invader {
        int x;
        int y;

        Invader(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Projectile {
        int x;
        int y;

        Projectile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
