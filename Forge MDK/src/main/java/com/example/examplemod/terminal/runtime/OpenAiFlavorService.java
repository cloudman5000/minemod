package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.ai.AiHackResult;
import com.example.examplemod.terminal.ai.AiHackService;
import com.example.examplemod.terminal.ai.OpenAiHackService;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class OpenAiFlavorService implements TerminalFlavorService {
    private final AiHackService ai = new OpenAiHackService();

    @Override
    public List<String> linesForCommand(ServerLevel level, String command, String context) {
        try {
            AiHackResult result = ai.execute(level, "Flavor only: " + command, context, "");
            return result.outputLines().stream().limit(2).map(line -> "[flavor] " + line).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
