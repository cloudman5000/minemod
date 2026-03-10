package com.example.examplemod.terminal.minigame;

import com.google.gson.Gson;

import java.util.List;

public final class PongTerminalMinigameEngine implements TerminalMinigameEngine {
    private static final Gson GSON = new Gson();
    private static final double WIDTH = 1.0;
    private static final double HEIGHT = 1.0;
    private static final double PADDLE_WIDTH = 0.22;
    private static final double PADDLE_Y = 0.94;
    private static final double MOVE_SPEED = 0.038;

    @Override
    public String kind() {
        return "pong";
    }

    @Override
    public MinigameTurnResult start(int targetScore) {
        PongState state = new PongState(
                0,
                0.5, 0.55,
                0.013, -0.016,
                0.5,
                3,
                0,
                false
        );
        return new MinigameTurnResult(
                GSON.toJson(state),
                state.score,
                false,
                false,
                List.of("[game] PONG_84 live controls: A/D or arrow keys")
        );
    }

    @Override
    public MinigameTurnResult step(MinigameInput input, String serializedState, int targetScore) {
        PongState state = deserialize(serializedState);
        state.paddle = clamp(state.paddle + (input.move() * MOVE_SPEED), PADDLE_WIDTH / 2.0, WIDTH - (PADDLE_WIDTH / 2.0));

        state.ballX += state.velX;
        state.ballY += state.velY;

        if (state.ballX <= 0.03 || state.ballX >= 0.97) {
            state.velX *= -1.0;
            state.ballX = clamp(state.ballX, 0.03, 0.97);
        }

        if (state.ballY <= 0.03) {
            state.velY = Math.abs(state.velY);
            state.ballY = 0.03;
        }

        boolean paddleRange = state.ballX >= (state.paddle - (PADDLE_WIDTH / 2.0))
                && state.ballX <= (state.paddle + (PADDLE_WIDTH / 2.0));
        if (state.ballY >= PADDLE_Y - 0.02 && state.velY > 0) {
            if (paddleRange) {
                state.velY = -Math.abs(state.velY) * 1.03;
                state.velX += (state.ballX - state.paddle) * 0.006;
                state.velX = clamp(state.velX, -0.028, 0.028);
                state.score += 1;
                state.ballY = PADDLE_Y - 0.02;
            } else if (state.ballY >= 0.99) {
                state.lives -= 1;
                if (state.lives > 0) {
                    resetBall(state);
                }
            }
        }
        state.tick++;

        boolean completed = state.score >= targetScore;
        boolean failed = state.lives <= 0;
        state.completed = completed;
        return new MinigameTurnResult(GSON.toJson(state), state.score, completed, failed, List.of());
    }

    private PongState deserialize(String raw) {
        try {
            PongState state = GSON.fromJson(raw, PongState.class);
            if (state != null) {
                return state;
            }
        } catch (Exception ignored) {
        }
        return new PongState(0, 0.5, 0.55, 0.013, -0.016, 0.5, 3, 0, false);
    }

    private void resetBall(PongState state) {
        state.ballX = 0.5;
        state.ballY = 0.55;
        state.velX = -state.velX;
        state.velY = -0.016;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class PongState {
        int tick;
        double ballX;
        double ballY;
        double velX;
        double velY;
        double paddle;
        int lives;
        int score;
        boolean completed;

        PongState(int tick, double ballX, double ballY, double velX, double velY, double paddle, int lives, int score, boolean completed) {
            this.tick = tick;
            this.ballX = ballX;
            this.ballY = ballY;
            this.velX = velX;
            this.velY = velY;
            this.paddle = paddle;
            this.lives = lives;
            this.score = score;
            this.completed = completed;
        }
    }
}
