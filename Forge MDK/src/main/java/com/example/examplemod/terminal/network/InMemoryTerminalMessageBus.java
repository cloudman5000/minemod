package com.example.examplemod.terminal.network;

import com.example.examplemod.terminal.domain.TerminalMessage;
import net.minecraft.server.level.ServerLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InMemoryTerminalMessageBus implements TerminalMessageBus {
    private static final InMemoryTerminalMessageBus INSTANCE = new InMemoryTerminalMessageBus();

    private final Map<String, Set<String>> knownTerminalsByDimension = new HashMap<>();
    private final Map<String, List<TerminalMessage>> inboxByTerminal = new HashMap<>();

    public static InMemoryTerminalMessageBus get() {
        return INSTANCE;
    }

    private static String dimensionKey(ServerLevel level) {
        return level.dimension().location().toString();
    }

    @Override
    public synchronized void register(ServerLevel level, String terminalId) {
        knownTerminalsByDimension.computeIfAbsent(dimensionKey(level), ignored -> new HashSet<>()).add(terminalId);
        inboxByTerminal.computeIfAbsent(terminalId, ignored -> new ArrayList<>());
    }

    @Override
    public synchronized void unregister(ServerLevel level, String terminalId) {
        Set<String> ids = knownTerminalsByDimension.get(dimensionKey(level));
        if (ids != null) {
            ids.remove(terminalId);
        }
        inboxByTerminal.remove(terminalId);
    }

    @Override
    public synchronized Set<String> listTerminalIds(ServerLevel level) {
        return Set.copyOf(knownTerminalsByDimension.getOrDefault(dimensionKey(level), Set.of()));
    }

    @Override
    public synchronized void send(ServerLevel level, TerminalMessage message) {
        inboxByTerminal.computeIfAbsent(message.toTerminalId(), ignored -> new ArrayList<>()).add(message);
    }

    @Override
    public synchronized void broadcast(ServerLevel level, String fromTerminalId, String channel, String body) {
        long now = Instant.now().toEpochMilli();
        for (String terminalId : listTerminalIds(level)) {
            if (terminalId.equals(fromTerminalId)) {
                continue;
            }
            TerminalMessage message = new TerminalMessage(fromTerminalId, terminalId, channel, body, now);
            inboxByTerminal.computeIfAbsent(terminalId, ignored -> new ArrayList<>()).add(message);
        }
    }

    @Override
    public synchronized List<TerminalMessage> drainInbox(ServerLevel level, String terminalId) {
        List<TerminalMessage> inbox = inboxByTerminal.getOrDefault(terminalId, new ArrayList<>());
        List<TerminalMessage> copy = List.copyOf(inbox);
        inbox.clear();
        return copy;
    }
}
