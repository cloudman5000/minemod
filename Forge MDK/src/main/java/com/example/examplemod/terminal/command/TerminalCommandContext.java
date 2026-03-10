package com.example.examplemod.terminal.command;

import com.example.examplemod.terminal.network.TerminalMessageBus;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class TerminalCommandContext {
    private final ServerLevel level;
    private final String terminalId;
    private final TerminalMessageBus messageBus;
    private final List<String> inboxSnapshot;

    public TerminalCommandContext(ServerLevel level, String terminalId, TerminalMessageBus messageBus, List<String> inboxSnapshot) {
        this.level = level;
        this.terminalId = terminalId;
        this.messageBus = messageBus;
        this.inboxSnapshot = inboxSnapshot;
    }

    public ServerLevel level() {
        return level;
    }

    public String terminalId() {
        return terminalId;
    }

    public TerminalMessageBus messageBus() {
        return messageBus;
    }

    public List<String> inboxSnapshot() {
        return inboxSnapshot;
    }
}
