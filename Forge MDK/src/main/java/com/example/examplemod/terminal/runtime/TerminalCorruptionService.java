package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.config.TerminalCorruptionConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public interface TerminalCorruptionService {
    CorruptionUpdate applyUsage(ServerLevel level, ServerPlayer player, int currentPoints, int delta, TerminalCorruptionConfig config);

    List<String> tickAmbient(ServerLevel level, ServerPlayer player, int stage, TerminalCorruptionConfig config, long gameTime);

    record CorruptionUpdate(int points, int stage, List<String> lines) {
    }
}
