package com.example.examplemod.terminal.ai;

import com.example.examplemod.terminal.domain.AiAvailabilityState;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class LocalFallbackAiHackService implements AiHackService {
    @Override
    public AiHackResult execute(ServerLevel level, String command, String terminalContext, String commandHistorySummary) {
        return new AiHackResult(
                List.of(
                        "[DEGRADED MODE] remote AI unavailable",
                        "limited local parser engaged for command: " + command
                ),
                AiAvailabilityState.DEGRADED
        );
    }
}
