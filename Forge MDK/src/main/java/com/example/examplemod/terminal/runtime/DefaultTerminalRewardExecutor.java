package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.config.TerminalRewardDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class DefaultTerminalRewardExecutor implements TerminalRewardExecutor {
    @Override
    public List<String> grant(ServerLevel level, ServerPlayer player, TerminalRewardDefinition reward) {
        List<String> output = new ArrayList<>();
        if (reward == null) {
            return List.of("[error] reward not found");
        }
        CommandSourceStack source = level.getServer()
                .createCommandSourceStack()
                .withSuppressedOutput()
                .withPermission(4);
        for (String raw : reward.commands()) {
            String cmd = personalize(raw, player.getName().getString());
            try {
                int result = level.getServer().getCommands().getDispatcher().execute(cmd, source);
                output.add("[reward] " + reward.title() + " :: /" + cmd + " (" + result + ")");
            } catch (Exception ex) {
                output.add("[error] reward command failed: /" + cmd + " (" + ex.getClass().getSimpleName() + ")");
            }
        }
        return output;
    }

    private String personalize(String command, String playerName) {
        String cmd = command == null ? "" : command.trim();
        if (cmd.isEmpty()) {
            return "";
        }
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        return cmd.replace("@p", playerName);
    }
}
