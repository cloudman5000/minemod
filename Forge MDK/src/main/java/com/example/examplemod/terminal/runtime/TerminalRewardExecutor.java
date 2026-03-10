package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.config.TerminalRewardDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public interface TerminalRewardExecutor {
    List<String> grant(ServerLevel level, ServerPlayer player, TerminalRewardDefinition reward);
}
