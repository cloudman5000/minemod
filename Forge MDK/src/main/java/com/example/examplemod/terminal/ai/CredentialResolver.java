package com.example.examplemod.terminal.ai;

import net.minecraft.server.level.ServerLevel;

public final class CredentialResolver {
    public TerminalAiConfig resolve(ServerLevel level) {
        TerminalAiConfig config = TerminalConfigLoader.loadOrCreate(level.getServer().getServerDirectory());
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return new TerminalAiConfig(envKey.trim(), config.model(), config.endpoint(), config.timeoutSeconds());
        }
        return config;
    }
}
