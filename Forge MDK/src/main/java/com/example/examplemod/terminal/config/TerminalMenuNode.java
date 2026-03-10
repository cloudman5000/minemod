package com.example.examplemod.terminal.config;

import java.util.List;

public record TerminalMenuNode(
        String id,
        String title,
        String description,
        String parentNodeId,
        List<TerminalMenuEntry> entries
) {
}
