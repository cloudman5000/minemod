package com.example.examplemod.terminal.runtime;

import com.example.examplemod.effect.ModEffects;
import com.example.examplemod.terminal.config.TerminalCorruptionConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

public final class DefaultTerminalCorruptionService implements TerminalCorruptionService {
    @Override
    public CorruptionUpdate applyUsage(ServerLevel level, ServerPlayer player, int currentPoints, int delta, TerminalCorruptionConfig config) {
        int points = Math.min(config.maxCorruption(), Math.max(0, currentPoints + Math.max(0, delta)));
        int stage = resolveStage(points, config.stageThresholds());

        List<String> lines = new ArrayList<>();

        if (player != null && stage >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, false));
        }
        if (player != null && stage >= 2) {
            player.addEffect(new MobEffectInstance(ModEffects.PARANOIA.getHolder().get(), 200, 0, false, true, true));
        }
        if (player != null && stage >= 3) {
            player.addEffect(new MobEffectInstance(ModEffects.DREAD.getHolder().get(), 240, 0, false, true, true));
        }
        if (player != null && stage >= 4) {
            player.addEffect(new MobEffectInstance(ModEffects.DELUSION.getHolder().get(), 260, 0, false, true, true));
            lines.add("[system] static surge detected in peripheral channels.");
        }

        return new CorruptionUpdate(points, stage, lines);
    }

    @Override
    public List<String> tickAmbient(ServerLevel level, ServerPlayer player, int stage, TerminalCorruptionConfig config, long gameTime) {
        if (stage <= 0) {
            return List.of();
        }
        int interval = Math.max(40, config.ambientSpawnIntervalTicks() - (stage * 20));
        if (gameTime % interval != 0) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (stage >= 1) {
            executeCommand(level, "summon examplemod:glitch ~" + (2 + stage) + " ~ ~");
            lines.add("[system] nearby signal interference detected");
        }
        if (stage >= 3) {
            executeCommand(level, "summon minecraft:lightning_bolt ~ ~ ~");
            lines.add("[system] atmospheric interference spike");
        }
        return lines;
    }

    private void executeCommand(ServerLevel level, String command) {
        try {
            level.getServer().getCommands().getDispatcher().execute(
                    command,
                    level.getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4)
            );
        } catch (Exception ignored) {
            // best-effort ambient behavior
        }
    }

    private int resolveStage(int points, List<Integer> thresholds) {
        int stage = 0;
        for (int threshold : thresholds) {
            if (points >= threshold) {
                stage++;
            }
        }
        return stage;
    }
}
