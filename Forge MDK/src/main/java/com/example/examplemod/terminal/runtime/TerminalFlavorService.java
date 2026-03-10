package com.example.examplemod.terminal.runtime;

import net.minecraft.server.level.ServerLevel;

import java.util.List;

public interface TerminalFlavorService {
    List<String> linesForCommand(ServerLevel level, String command, String context);
}
