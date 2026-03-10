package com.example.examplemod.terminal.domain;

import java.time.Instant;

public record TerminalMessage(
        String fromTerminalId,
        String toTerminalId,
        String channel,
        String body,
        long createdAtEpochMs
) {
    public static TerminalMessage direct(String from, String to, String body) {
        return new TerminalMessage(from, to, "direct", body, Instant.now().toEpochMilli());
    }
}
