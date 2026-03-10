package com.example.examplemod.terminal.network;

import com.example.examplemod.terminal.domain.TerminalMessage;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Set;

public interface TerminalMessageBus {
    void register(ServerLevel level, String terminalId);

    void unregister(ServerLevel level, String terminalId);

    Set<String> listTerminalIds(ServerLevel level);

    void send(ServerLevel level, TerminalMessage message);

    void broadcast(ServerLevel level, String fromTerminalId, String channel, String body);

    List<TerminalMessage> drainInbox(ServerLevel level, String terminalId);
}
