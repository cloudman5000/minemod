package com.example.examplemod.terminal.ai;

import net.minecraft.server.level.ServerLevel;

public interface AiHackService {
    AiHackResult execute(ServerLevel level, String command, String terminalContext, String commandHistorySummary);
}
