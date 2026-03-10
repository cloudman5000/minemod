package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.command.TerminalCommandContext;
import com.example.examplemod.terminal.command.TerminalCommandDispatcher;
import com.example.examplemod.terminal.command.TerminalCommandResult;
import com.example.examplemod.terminal.config.TerminalCorruptionConfig;
import com.example.examplemod.terminal.config.TerminalGameplayConfig;
import com.example.examplemod.terminal.config.TerminalGameplayConfigLoader;
import com.example.examplemod.terminal.config.TerminalMenuEntry;
import com.example.examplemod.terminal.config.TerminalMenuEntryType;
import com.example.examplemod.terminal.config.TerminalMenuNode;
import com.example.examplemod.terminal.config.TerminalMinigameDefinition;
import com.example.examplemod.terminal.config.TerminalRewardDefinition;
import com.example.examplemod.terminal.config.TerminalScriptDefinition;
import com.example.examplemod.terminal.domain.AiAvailabilityState;
import com.example.examplemod.terminal.domain.TerminalActivity;
import com.example.examplemod.terminal.domain.TerminalMessage;
import com.example.examplemod.terminal.domain.TerminalState;
import com.example.examplemod.terminal.domain.WorkflowState;
import com.example.examplemod.terminal.minigame.MinigameTurnResult;
import com.example.examplemod.terminal.minigame.MinigameInput;
import com.example.examplemod.terminal.minigame.TerminalMinigameEngine;
import com.example.examplemod.terminal.minigame.TerminalMinigameRegistry;
import com.example.examplemod.terminal.network.InMemoryTerminalMessageBus;
import com.example.examplemod.terminal.network.TerminalMessageBus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TerminalRuntimeEngine {
    private static final int MAX_HISTORY = 500;
    private static final String[] THINKING_FRAMES = {".", "..", "...", "...."};

    private final TerminalCommandDispatcher commandDispatcher;
    private final TerminalMessageBus messageBus;
    private final TerminalRewardExecutor rewardExecutor;
    private final TerminalCorruptionService corruptionService;
    private final TerminalFlavorService flavorService;
    private final TerminalMinigameRegistry minigameRegistry;

    private final List<WorkflowEvent> pendingEvents = new ArrayList<>();
    private final Map<String, Map<String, Long>> rewardCooldownsByTerminal = new HashMap<>();
    private int pendingEventIndex = 0;
    private long nextEventGameTime = -1L;
    private int thinkingFrame = 0;

    public TerminalRuntimeEngine() {
        this(
                new TerminalCommandDispatcher(),
                InMemoryTerminalMessageBus.get(),
                new DefaultTerminalRewardExecutor(),
                new DefaultTerminalCorruptionService(),
                new OpenAiFlavorService(),
                new TerminalMinigameRegistry()
        );
    }

    public TerminalRuntimeEngine(
            TerminalCommandDispatcher commandDispatcher,
            TerminalMessageBus messageBus,
            TerminalRewardExecutor rewardExecutor,
            TerminalCorruptionService corruptionService,
            TerminalFlavorService flavorService,
            TerminalMinigameRegistry minigameRegistry
    ) {
        this.commandDispatcher = commandDispatcher;
        this.messageBus = messageBus;
        this.rewardExecutor = rewardExecutor;
        this.corruptionService = corruptionService;
        this.flavorService = flavorService;
        this.minigameRegistry = minigameRegistry;
    }

    public void submitCommand(
            TerminalStateStore stateStore,
            ServerLevel level,
            String terminalId,
            UUID operatorUuid,
            String command
    ) {
        TerminalState state = stateStore.load();
        TerminalGameplayConfig config = TerminalGameplayConfigLoader.load(level.getServer().getServerDirectory());
        String input = normalizeSubmittedInput(command);
        if (input.isBlank()) {
            return;
        }
        if (isArcadeControlCommand(input) && tryConsumeArcadeControl(state, input)) {
            stateStore.save(state);
            return;
        }

        state.setAiAvailabilityState(config.aiFlavorEnabled() ? AiAvailabilityState.ONLINE : AiAvailabilityState.DISABLED);
        state.setActiveOperatorUuid(operatorUuid.toString());
        appendLine(state, "root@" + terminalId + "> " + input);

        if (isMinigameExit(input)) {
            state.setActiveMinigameId("");
            state.setActiveMinigameKind("");
            state.setMinigameStateJson("");
            appendLine(state, "[game] minigame session terminated");
            stateStore.save(state);
            return;
        }

        TerminalCommandResult builtIn = commandDispatcher.dispatch(input, new TerminalCommandContext(level, terminalId, messageBus, List.copyOf(state.inbox())));
        if (builtIn.handled()) {
            builtIn.outputLines().forEach(line -> appendLine(state, line));
            stateStore.save(state);
            return;
        }

        processTerminalCommand(state, level, terminalId, config, input);
        stateStore.save(state);
    }

    static String normalizeSubmittedInput(String command) {
        String normalized = command == null ? "" : command.trim();
        if (normalized.length() >= 2) {
            char first = normalized.charAt(0);
            char last = normalized.charAt(normalized.length() - 1);
            boolean doubleQuoted = first == '"' && last == '"';
            boolean singleQuoted = first == '\'' && last == '\'';
            if (doubleQuoted || singleQuoted) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        return normalized;
    }

    public void tick(TerminalStateStore stateStore, ServerLevel level, String terminalId) {
        TerminalState state = stateStore.load();
        TerminalGameplayConfig config = TerminalGameplayConfigLoader.load(level.getServer().getServerDirectory());
        messageBus.register(level, terminalId);

        List<TerminalMessage> inbox = messageBus.drainInbox(level, terminalId);
        for (TerminalMessage msg : inbox) {
            String line = "[msg:" + msg.channel() + "] " + msg.fromTerminalId() + " -> " + msg.body();
            state.inbox().add(line);
            appendLine(state, line);
        }

        if (state.workflowState() != null && state.workflowState().running() && pendingEventIndex < pendingEvents.size()) {
            if (nextEventGameTime < 0 || level.getGameTime() >= nextEventGameTime) {
                WorkflowEvent event = pendingEvents.get(pendingEventIndex++);
                applyWorkflowEvent(event, level, state);
                nextEventGameTime = level.getGameTime() + Math.max(1L, event.delayTicks());
                if (pendingEventIndex >= pendingEvents.size()) {
                    completeWorkflow(state);
                    pendingEvents.clear();
                    pendingEventIndex = 0;
                    nextEventGameTime = -1L;
                    appendLine(state, "[live:terminal.processing|");
                }
            }
        } else if (state.workflowState() != null && state.workflowState().running()) {
            appendLine(state, "[live:terminal.processing|processing " + THINKING_FRAMES[thinkingFrame] + "]");
            thinkingFrame = (thinkingFrame + 1) % THINKING_FRAMES.length;
        }

        if (!state.activeMinigameId().isBlank()) {
            tickMinigameFrame(state, level, terminalId, config);
        }

        if (state.workflowState() != null && state.workflowState().running()
                && state.pendingUntilGameTime() > 0
                && level.getGameTime() > state.pendingUntilGameTime()) {
            cancelPendingActivity(state, "workflow timed out");
            pendingEvents.clear();
            pendingEventIndex = 0;
            nextEventGameTime = -1L;
        }

        ServerPlayer player = activePlayer(level, state);
        if (player != null) {
            List<String> ambient = corruptionService.tickAmbient(level, player, state.corruptionStage(), config.corruption(), level.getGameTime());
            ambient.forEach(line -> appendLine(state, line));
        }

        stateStore.save(state);
    }

    public void terminatePendingActivity(TerminalStateStore stateStore, String reason) {
        TerminalState state = stateStore.load();
        cancelPendingActivity(state, reason);
        stateStore.save(state);
        pendingEvents.clear();
        pendingEventIndex = 0;
        nextEventGameTime = -1L;
    }

    private void processTerminalCommand(TerminalState state, ServerLevel level, String terminalId, TerminalGameplayConfig config, String input) {
        String lower = input.toLowerCase();
        if ("menu".equals(lower) || "home".equals(lower)) {
            state.setActiveNodeId(config.rootNodeId());
            renderCurrentNode(state, config);
            return;
        }
        if ("ls".equals(lower) || "list".equals(lower)) {
            renderCurrentNode(state, config);
            return;
        }
        if ("back".equals(lower)) {
            TerminalMenuNode node = config.nodes().get(state.activeNodeId());
            if (node != null && node.parentNodeId() != null && !node.parentNodeId().isBlank()) {
                state.setActiveNodeId(node.parentNodeId());
            } else {
                state.setActiveNodeId(config.rootNodeId());
            }
            renderCurrentNode(state, config);
            return;
        }
        if (lower.startsWith("cd ")) {
            String target = input.substring(3).trim();
            if (canNavigateToNode(state, config, target)) {
                state.setActiveNodeId(target);
                renderCurrentNode(state, config);
            } else {
                appendLine(state, "[error] path locked or unknown: " + target);
            }
            return;
        }
        if (lower.startsWith("open ")) {
            String entryId = input.substring(5).trim();
            openEntryById(state, level, terminalId, config, entryId);
            return;
        }
        if (lower.startsWith("run ")) {
            appendLine(state, "[error] direct app launch is locked. navigate menus and `open <id>`");
            return;
        }
        if (lower.startsWith("play ")) {
            appendLine(state, "[error] direct game launch is locked. navigate menus and `open <id>`");
            return;
        }
        if ("rewards".equals(lower)) {
            appendLine(state, "[error] reward index unavailable. explore the terminal tree.");
            return;
        }
        if (lower.startsWith("claim ")) {
            appendLine(state, "[error] direct reward claim is locked. navigate menus and `open <id>`");
            return;
        }

        openEntryById(state, level, terminalId, config, input);
    }

    private void renderCurrentNode(TerminalState state, TerminalGameplayConfig config) {
        TerminalMenuNode node = config.nodes().get(state.activeNodeId());
        if (node == null) {
            appendLine(state, "[error] active node missing: " + state.activeNodeId());
            return;
        }
        appendLine(state, "[system] " + node.title() + " :: " + node.description());
        for (TerminalMenuEntry entry : node.entries()) {
            boolean locked = !flagSatisfied(state, entry.requiredFlag());
            String lockSuffix = locked ? " [locked]" : "";
            appendLine(state, "[system] - " + entry.id() + " :: " + entry.label() + lockSuffix + " (" + entry.type() + ")");
        }
        appendLine(state, "[system] commands: open <id>, back, menu, ls");
    }

    private void openEntryById(TerminalState state, ServerLevel level, String terminalId, TerminalGameplayConfig config, String entryId) {
        TerminalMenuNode node = config.nodes().get(state.activeNodeId());
        if (node == null) {
            appendLine(state, "[error] active menu node unavailable");
            return;
        }
        TerminalMenuEntry found = node.entries().stream()
                .filter(entry -> entry.id().equalsIgnoreCase(entryId))
                .findFirst()
                .orElse(null);
        if (found == null) {
            appendLine(state, "[error] unknown menu entry: " + entryId);
            return;
        }
        if (!flagSatisfied(state, found.requiredFlag())) {
            appendLine(state, "[error] access denied: requirements unmet for " + found.id());
            return;
        }
        consumeCorruption(state, level, config.corruption(), found.corruptionCost());
        switch (found.type()) {
            case NODE -> {
                state.setActiveNodeId(found.target());
                renderCurrentNode(state, config);
            }
            case APP -> {
                startScriptById(state, level, terminalId, config, found.target());
                grantFlag(state, found.grantsFlag());
            }
            case MINIGAME -> startMinigame(state, level, config, found.target());
            case REWARD -> claimReward(state, level, terminalId, config, found.target());
        }
    }

    private void startScriptById(TerminalState state, ServerLevel level, String terminalId, TerminalGameplayConfig config, String appId) {
        TerminalScriptDefinition script = config.apps().get(appId);
        if (script == null) {
            appendLine(state, "[error] unknown app script: " + appId);
            return;
        }
        startWorkflow(state, level, "app:" + script.id(), script.id(), script.lines(), terminalId, config.aiFlavorEnabled(), config.corruption());
        grantFlag(state, script.grantsFlag());
    }

    private void startWorkflow(
            TerminalState state,
            ServerLevel level,
            String contextId,
            String command,
            List<String> lines,
            String terminalId,
            boolean includeFlavor,
            TerminalCorruptionConfig corruptionConfig
    ) {
        pendingEvents.clear();
        pendingEventIndex = 0;
        nextEventGameTime = level.getGameTime() + 1L;
        appendLine(state, "[live:terminal.processing|processing .]");

        for (String line : lines) {
            queueDirectiveLine(line);
        }

        if (includeFlavor) {
            String context = "terminal=" + terminalId + ",node=" + state.activeNodeId();
            for (String line : flavorService.linesForCommand(level, command, context)) {
                queueDirectiveLine(line);
            }
        }

        if (pendingEvents.isEmpty()) {
            appendLine(state, "[system] no output");
            return;
        }
        String workflowId = "wf-" + System.currentTimeMillis();
        state.setWorkflowState(new WorkflowState(workflowId, command, contextId, 0, level.getGameTime(), true));
        state.setActivity(TerminalActivity.RUNNING_WORKFLOW);
        state.setPendingUntilGameTime(level.getGameTime() + 20L * 90L);
    }

    private void startMinigame(TerminalState state, ServerLevel level, TerminalGameplayConfig config, String minigameId) {
        TerminalMinigameDefinition minigame = config.minigames().get(minigameId);
        if (minigame == null) {
            appendLine(state, "[error] unknown minigame: " + minigameId);
            return;
        }
        TerminalMinigameEngine engine = minigameRegistry.resolve(minigame.kind());
        MinigameTurnResult start = engine.start(minigame.targetScore());
        state.setActiveMinigameId(minigame.id());
        state.setActiveMinigameKind(minigame.kind());
        state.setMinigameStateJson(start.stateJson());
        appendLine(state, "[game] launching " + minigame.title());
        start.outputLines().forEach(line -> appendLine(state, line));
        consumeCorruption(state, level, config.corruption(), minigame.corruptionCost());
    }

    private void tickMinigameFrame(TerminalState state, ServerLevel level, String terminalId, TerminalGameplayConfig config) {
        TerminalMinigameDefinition minigame = config.minigames().get(state.activeMinigameId());
        if (minigame == null) {
            state.setActiveMinigameId("");
            state.setActiveMinigameKind("");
            state.setMinigameStateJson("");
            appendLine(state, "[error] active minigame no longer exists");
            return;
        }
        TerminalMinigameEngine engine = minigameRegistry.resolve(minigame.kind());
        MinigameInput input = MinigameInput.of(state.arcadeMoveIntent(), state.arcadeFireQueued());
        state.setArcadeFireQueued(false);
        MinigameTurnResult turn = engine.step(input, state.minigameStateJson(), minigame.targetScore());
        state.setMinigameStateJson(turn.stateJson());
        if (!turn.outputLines().isEmpty()) {
            turn.outputLines().forEach(line -> appendLine(state, line));
        }
        if (turn.completed()) {
            appendLine(state, "[game] " + minigame.title() + " completed");
            state.setActiveMinigameId("");
            state.setActiveMinigameKind("");
            state.setMinigameStateJson("");
            grantFlag(state, minigame.grantsFlag());
            if (minigame.rewardId() != null && !minigame.rewardId().isBlank()) {
                claimReward(state, level, terminalId, config, minigame.rewardId());
            }
        } else if (turn.failed()) {
            appendLine(state, "[game] " + minigame.title() + " failed");
            state.setActiveMinigameId("");
            state.setActiveMinigameKind("");
            state.setMinigameStateJson("");
        }
    }

    private void claimReward(TerminalState state, ServerLevel level, String terminalId, TerminalGameplayConfig config, String rewardId) {
        TerminalRewardDefinition reward = config.rewards().get(rewardId);
        if (reward == null) {
            appendLine(state, "[error] reward not found: " + rewardId);
            return;
        }
        if (!flagSatisfied(state, reward.requiredFlag())) {
            appendLine(state, "[error] reward remains locked: " + reward.id());
            return;
        }
        long now = level.getGameTime();
        Map<String, Long> cooldowns = rewardCooldownsByTerminal.computeIfAbsent(terminalId, ignored -> new HashMap<>());
        long until = cooldowns.getOrDefault(reward.id(), -1L);
        if (until > now) {
            appendLine(state, "[error] reward cooldown active: " + reward.id() + " (" + ((until - now) / 20) + "s)");
            return;
        }
        ServerPlayer player = activePlayer(level, state);
        if (player == null) {
            appendLine(state, "[error] no active player for reward execution");
            return;
        }
        rewardExecutor.grant(level, player, reward).forEach(line -> appendLine(state, line));
        cooldowns.put(reward.id(), now + (reward.cooldownSeconds() * 20L));
        consumeCorruption(state, level, config.corruption(), reward.corruptionCost());
    }

    private void consumeCorruption(TerminalState state, ServerLevel level, TerminalCorruptionConfig config, int delta) {
        ServerPlayer player = activePlayer(level, state);
        if (player == null) {
            state.setCorruptionPoints(Math.min(config.maxCorruption(), state.corruptionPoints() + Math.max(0, delta)));
            return;
        }
        TerminalCorruptionService.CorruptionUpdate update = corruptionService.applyUsage(level, player, state.corruptionPoints(), delta, config);
        state.setCorruptionPoints(update.points());
        state.setCorruptionStage(update.stage());
    }

    boolean tryConsumeArcadeControl(TerminalState state, String input) {
        if (!isArcadeControlCommand(input)) {
            return false;
        }
        if (state.activeMinigameId() == null || state.activeMinigameId().isBlank()) {
            return true;
        }
        String[] parts = input.split("\\s+");
        if (parts.length >= 3 && "move".equalsIgnoreCase(parts[1])) {
            state.setArcadeMoveIntent(parseIntSafe(parts[2], 0));
            return true;
        }
        if (parts.length >= 2 && "fire".equalsIgnoreCase(parts[1])) {
            state.setArcadeFireQueued(true);
            return true;
        }
        return true;
    }

    static boolean isArcadeControlCommand(String input) {
        return input != null && input.trim().toLowerCase().startsWith("__arcade");
    }

    private boolean canNavigateToNode(TerminalState state, TerminalGameplayConfig config, String targetNodeId) {
        TerminalMenuNode node = config.nodes().get(state.activeNodeId());
        if (node == null) {
            return false;
        }
        return node.entries().stream()
                .anyMatch(entry -> entry.type() == TerminalMenuEntryType.NODE
                        && entry.target().equalsIgnoreCase(targetNodeId)
                        && flagSatisfied(state, entry.requiredFlag()));
    }

    private boolean flagSatisfied(TerminalState state, String requiredFlag) {
        if (requiredFlag == null || requiredFlag.isBlank()) {
            return true;
        }
        return state.progressionFlags().contains(requiredFlag);
    }

    private void grantFlag(TerminalState state, String flag) {
        if (flag == null || flag.isBlank()) {
            return;
        }
        state.progressionFlags().add(flag);
    }

    private void queueDirectiveLine(String raw) {
        String line = raw == null ? "" : raw.trim();
        if (line.isEmpty()) {
            return;
        }
        String lower = line.toLowerCase();
        if (lower.startsWith("wait:")) {
            pendingEvents.add(WorkflowEvent.waitOnly(msToTicks(parseIntSafe(line.substring(5).trim(), 500))));
            return;
        }
        if (lower.startsWith("mc:")) {
            pendingEvents.add(WorkflowEvent.command(line.substring(3).trim(), 10L));
            return;
        }
        if (lower.startsWith("tts:")) {
            pendingEvents.add(WorkflowEvent.voice(line.substring(4).trim(), 8L));
            return;
        }
        if (lower.startsWith("progress:")) {
            pendingEvents.addAll(parseProgressEvents(line.substring(9).trim()));
            return;
        }
        if (lower.startsWith("spinner:")) {
            pendingEvents.addAll(parseSpinnerEvents(line.substring(8).trim()));
            return;
        }
        if (lower.startsWith("tree:")) {
            pendingEvents.addAll(parseTreeEvents(line.substring(5).trim()));
            return;
        }
        if (lower.startsWith("live:")) {
            String payload = line.substring(5).trim();
            String[] parts = payload.split("\\|", 2);
            String id = parts.length > 0 ? parts[0].trim() : "status";
            String text = parts.length > 1 ? parts[1].trim() : "";
            pendingEvents.add(WorkflowEvent.live(id, text, 8L));
            return;
        }
        pendingEvents.add(WorkflowEvent.text(line, 10L));
    }

    private List<WorkflowEvent> parseProgressEvents(String payload) {
        String[] parts = payload.split("\\|", 2);
        String label = parts.length > 0 ? parts[0].trim() : "loading";
        int seconds = parts.length > 1 ? parseIntSafe(parts[1].trim(), 5) : 5;
        seconds = Math.max(1, Math.min(seconds, 30));
        int steps = 10;
        long delayTicks = Math.max(1L, (seconds * 20L) / steps);
        String liveId = "progress." + sanitizeLiveId(label);

        List<WorkflowEvent> events = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            int percent = (i * 100) / steps;
            int barCells = 18;
            int filled = (percent * barCells) / 100;
            String bar = "[" + "#".repeat(Math.max(0, filled)) + "-".repeat(Math.max(0, barCells - filled)) + "]";
            events.add(WorkflowEvent.live(liveId, label + " " + bar + " " + percent + "%", delayTicks));
        }
        events.add(WorkflowEvent.live(liveId, "", 1L));
        return events;
    }

    private List<WorkflowEvent> parseSpinnerEvents(String payload) {
        String[] parts = payload.split("\\|", 2);
        String label = parts.length > 0 ? parts[0].trim() : "calibrating";
        int seconds = parts.length > 1 ? parseIntSafe(parts[1].trim(), 4) : 4;
        seconds = Math.max(1, Math.min(seconds, 30));
        String id = "spin." + sanitizeLiveId(label);
        String[] frames = {"|", "/", "-", "\\"};
        int ticks = seconds * 20;
        int frameDelay = 4;
        List<WorkflowEvent> events = new ArrayList<>();
        for (int t = 0; t < ticks; t += frameDelay) {
            String frame = frames[(t / frameDelay) % frames.length];
            int percent = Math.min(100, (t * 100) / Math.max(1, ticks - frameDelay));
            events.add(WorkflowEvent.live(id, label + " " + frame + " " + percent + "%", frameDelay));
        }
        events.add(WorkflowEvent.live(id, "", 1L));
        return events;
    }

    private List<WorkflowEvent> parseTreeEvents(String payload) {
        String[] parts = payload.split("\\|", 2);
        String root = parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : "sys";
        int depth = parts.length > 1 ? parseIntSafe(parts[1].trim(), 3) : 3;
        depth = Math.max(1, Math.min(depth, 6));
        List<WorkflowEvent> events = new ArrayList<>();
        events.add(WorkflowEvent.text("[game] " + root + "/", 8L));
        for (int d = 1; d <= depth; d++) {
            String indent = "  ".repeat(d - 1);
            events.add(WorkflowEvent.text("[game] " + indent + "|-- kernel_" + d + ".bin", 8L));
            events.add(WorkflowEvent.text("[game] " + indent + "|-- daemon_" + d + ".svc", 8L));
            events.add(WorkflowEvent.text("[game] " + indent + "`-- ghost_proc_" + d, 8L));
        }
        return events;
    }

    private void applyWorkflowEvent(WorkflowEvent event, ServerLevel level, TerminalState state) {
        switch (event.type()) {
            case TEXT -> appendLine(state, event.payload());
            case VOICE -> appendLine(state, "[voice] " + event.payload());
            case COMMAND -> executeCommand(level, state, event.payload());
            case LIVE -> appendLine(state, "[live:" + event.liveId() + "|" + event.payload());
            case WAIT -> {
                // no-op
            }
        }
    }

    private void executeCommand(ServerLevel level, TerminalState state, String commandText) {
        String command = sanitizeCommand(commandText);
        if (command.isBlank()) {
            return;
        }
        try {
            CommandSourceStack source = level.getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4);
            int result = level.getServer().getCommands().getDispatcher().execute(command, source);
            appendLine(state, "cmd> /" + command + " (" + result + ")");
        } catch (Exception e) {
            appendLine(state, "[error] command failed /" + command + ": " + e.getClass().getSimpleName());
        }
    }

    private String sanitizeCommand(String commandText) {
        String command = commandText == null ? "" : commandText.trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        while (command.endsWith(";")) {
            command = command.substring(0, command.length() - 1).trim();
        }
        return command
                .replace("minecraft:glitch_entity", "examplemod:glitch")
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2019', '\'')
                .replace('\u2018', '\'');
    }

    private boolean isMinigameExit(String input) {
        String lower = input.toLowerCase();
        return "quit".equals(lower) || "stop".equals(lower) || "exit".equals(lower);
    }

    private ServerPlayer activePlayer(ServerLevel level, TerminalState state) {
        try {
            if (state.activeOperatorUuid() == null || state.activeOperatorUuid().isBlank()) {
                return null;
            }
            return level.getServer().getPlayerList().getPlayer(UUID.fromString(state.activeOperatorUuid()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void completeWorkflow(TerminalState state) {
        if (state.workflowState() != null) {
            state.workflowState().setRunning(false);
        }
        state.setPendingUntilGameTime(-1L);
        state.setActivity(TerminalActivity.IDLE);
    }

    private static void cancelPendingActivity(TerminalState state, String reason) {
        if (state.workflowState() != null) {
            state.workflowState().setRunning(false);
        }
        state.setPendingUntilGameTime(-1L);
        state.setActivity(TerminalActivity.TERMINATED);
        state.setWorkflowState(null);
        state.setActiveMinigameId("");
        state.setMinigameStateJson("");
        appendLine(state, "pending activity reset: " + reason);
        state.setActivity(TerminalActivity.IDLE);
    }

    private static int parseIntSafe(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long msToTicks(int millis) {
        return Math.max(1L, millis / 50L);
    }

    private static String sanitizeLiveId(String text) {
        String normalized = text == null ? "status" : text.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9._-]", "_");
        return normalized.isBlank() ? "status" : normalized;
    }

    private static void appendLine(TerminalState state, String line) {
        state.history().add(line);
        while (state.history().size() > MAX_HISTORY) {
            state.history().removeFirst();
        }
    }

    private enum WorkflowEventType {
        TEXT,
        VOICE,
        COMMAND,
        LIVE,
        WAIT
    }

    private record WorkflowEvent(WorkflowEventType type, String payload, String liveId, long delayTicks) {
        static WorkflowEvent text(String payload, long delayTicks) {
            return new WorkflowEvent(WorkflowEventType.TEXT, payload, "", delayTicks);
        }

        static WorkflowEvent voice(String payload, long delayTicks) {
            return new WorkflowEvent(WorkflowEventType.VOICE, payload, "", delayTicks);
        }

        static WorkflowEvent command(String payload, long delayTicks) {
            return new WorkflowEvent(WorkflowEventType.COMMAND, payload, "", delayTicks);
        }

        static WorkflowEvent live(String liveId, String payload, long delayTicks) {
            return new WorkflowEvent(WorkflowEventType.LIVE, payload, liveId, delayTicks);
        }

        static WorkflowEvent waitOnly(long delayTicks) {
            return new WorkflowEvent(WorkflowEventType.WAIT, "", "", delayTicks);
        }
    }
}
