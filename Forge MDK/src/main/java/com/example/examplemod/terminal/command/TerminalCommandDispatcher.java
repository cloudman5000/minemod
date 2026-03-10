package com.example.examplemod.terminal.command;

import com.example.examplemod.terminal.domain.TerminalMessage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerminalCommandDispatcher {
    private final Map<String, TerminalCommand> commandMap = new HashMap<>();

    public TerminalCommandDispatcher() {
        commandMap.put("help", (context, args) -> TerminalCommandResult.handled(List.of(
                "commands: help, menu, ls, open <id>, back, cd <id>",
                "network/tools: status, terminals, link, send, broadcast, inbox, history, connect, workflows, cancel, exit",
                "tip: explore deeply - the arcade and rewards unlock through discoveries"
        )));
        commandMap.put("exit", (context, args) -> TerminalCommandResult.close(List.of("closing terminal...")));
        commandMap.put("status", (context, args) -> TerminalCommandResult.handled(List.of(
                "terminal id: " + context.terminalId(),
                "known terminals: " + context.messageBus().listTerminalIds(context.level()).size()
        )));
        commandMap.put("terminals", (context, args) -> TerminalCommandResult.handled(
                context.messageBus().listTerminalIds(context.level()).stream()
                        .sorted()
                        .map(id -> "- " + id)
                        .toList()
        ));
        commandMap.put("inbox", (context, args) -> {
            if (context.inboxSnapshot().isEmpty()) {
                return TerminalCommandResult.handled(List.of("inbox is empty"));
            }
            return TerminalCommandResult.handled(context.inboxSnapshot());
        });
        commandMap.put("send", (context, args) -> {
            if (args.size() < 2) {
                return TerminalCommandResult.handled(List.of("usage: send <terminalId> <message>"));
            }
            String targetId = args.get(0);
            String body = String.join(" ", args.subList(1, args.size()));
            context.messageBus().send(context.level(), TerminalMessage.direct(context.terminalId(), targetId, body));
            return TerminalCommandResult.handled(List.of("message sent to " + targetId));
        });
        commandMap.put("broadcast", (context, args) -> {
            if (args.size() < 2) {
                return TerminalCommandResult.handled(List.of("usage: broadcast <channel> <message>"));
            }
            String channel = args.get(0);
            String body = String.join(" ", args.subList(1, args.size()));
            context.messageBus().broadcast(context.level(), context.terminalId(), channel, body);
            return TerminalCommandResult.handled(List.of("broadcast sent on channel " + channel));
        });
        commandMap.put("link", (context, args) -> TerminalCommandResult.handled(List.of(
                args.isEmpty() ? "usage: link <terminalId>" : "link target set: " + args.getFirst()
        )));
        commandMap.put("connect", (context, args) -> TerminalCommandResult.handled(List.of(
                args.isEmpty() ? "usage: connect <target>" : "attempting connection to " + args.getFirst()
        )));
        commandMap.put("history", (context, args) -> TerminalCommandResult.handled(List.of("history command acknowledged")));
        commandMap.put("workflows", (context, args) -> TerminalCommandResult.handled(List.of("workflows: managed by runtime engine")));
        commandMap.put("cancel", (context, args) -> TerminalCommandResult.handled(List.of("cancel requested")));
    }

    public TerminalCommandResult dispatch(String rawCommand, TerminalCommandContext context) {
        String normalized = rawCommand == null ? "" : rawCommand.trim();
        if (normalized.isEmpty()) {
            return TerminalCommandResult.handled(List.of());
        }
        List<String> tokens = Arrays.asList(normalized.split("\\s+"));
        String root = tokens.getFirst().toLowerCase();
        TerminalCommand command = commandMap.get(root);
        if (command == null) {
            return TerminalCommandResult.notHandled();
        }
        List<String> args = tokens.size() > 1 ? tokens.subList(1, tokens.size()) : List.of();
        return command.execute(context, args);
    }
}
