package com.example.examplemod.terminal.runtime;

import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class NoopTerminalFlavorService implements TerminalFlavorService {
    @Override
    public List<String> linesForCommand(ServerLevel level, String command, String context) {
        return List.of();
    }
}
