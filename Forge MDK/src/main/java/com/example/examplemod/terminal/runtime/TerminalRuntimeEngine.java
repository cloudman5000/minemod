package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.ai.AiHackResult;
import com.example.examplemod.terminal.ai.AiHackService;
import com.example.examplemod.terminal.ai.OpenAiHackService;
import com.example.examplemod.terminal.command.TerminalCommandContext;
import com.example.examplemod.terminal.command.TerminalCommandDispatcher;
import com.example.examplemod.terminal.command.TerminalCommandResult;
import com.example.examplemod.terminal.domain.AiAvailabilityState;
import com.example.examplemod.terminal.domain.TerminalActivity;
import com.example.examplemod.terminal.domain.TerminalMessage;
import com.example.examplemod.terminal.domain.TerminalState;
import com.example.examplemod.terminal.domain.WorkflowState;
import com.example.examplemod.terminal.intent.IntentClassificationResult;
import com.example.examplemod.terminal.intent.TerminalIntentRouter;
import com.example.examplemod.terminal.intent.TerminalIntentType;
import com.example.examplemod.terminal.minigame.MinigameChallengeService;
import com.example.examplemod.terminal.minigame.SimpleMinigameChallengeService;
import com.example.examplemod.terminal.network.InMemoryTerminalMessageBus;
import com.example.examplemod.terminal.network.TerminalMessageBus;
import com.example.examplemod.terminal.policy.HackPolicyEngine;
import com.example.examplemod.terminal.policy.ReconGateHackPolicyEngine;
import com.example.examplemod.terminal.sequence.SimpleTerminalSequenceEngine;
import com.example.examplemod.terminal.sequence.TerminalSequenceEngine;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TerminalRuntimeEngine {
    private static final int MAX_HISTORY = 300;
    private static final String[] THINKING_FRAMES = {".", "..", "...", "...."};

    private final TerminalIntentRouter intentRouter = new TerminalIntentRouter();
    private final TerminalCommandDispatcher commandDispatcher = new TerminalCommandDispatcher();
    private final TerminalMessageBus messageBus = InMemoryTerminalMessageBus.get();
    private final AiHackService aiHackService = new OpenAiHackService();
    private final TerminalSequenceEngine sequenceEngine = new SimpleTerminalSequenceEngine();
    private final HackPolicyEngine hackPolicyEngine = new ReconGateHackPolicyEngine();
    private final MinigameChallengeService minigameChallengeService = new SimpleMinigameChallengeService();

    private CompletableFuture<AiHackResult> pendingAiFuture;
    private String pendingWorkflowId = "";
    private final List<WorkflowEvent> pendingEvents = new ArrayList<>();
    private int pendingEventIndex = 0;
    private long nextEventGameTime = -1L;
    private long nextThinkingTick = -1L;
    private int thinkingFrame = 0;

    public void submitCommand(
            TerminalStateStore stateStore,
            ServerLevel level,
            String terminalId,
            UUID operatorUuid,
            String command
    ) {
        TerminalState state = stateStore.load();
        state.setActiveOperatorUuid(operatorUuid.toString());
        appendLine(state, "root@" + terminalId + "> " + command);

        IntentClassificationResult classification = intentRouter.classify(command);

        if (classification.type() == TerminalIntentType.HARDCODED_COMMAND
                || classification.type() == TerminalIntentType.WORKFLOW_CONTROL) {
            TerminalCommandContext ctx = new TerminalCommandContext(
                    level,
                    terminalId,
                    messageBus,
                    List.copyOf(state.inbox())
            );
            TerminalCommandResult result = commandDispatcher.dispatch(command, ctx);
            if (result.handled()) {
                result.outputLines().forEach(line -> appendLine(state, line));
                if (result.closeScreen()) {
                    state.setActivity(TerminalActivity.IDLE);
                }
                if ("cancel".equalsIgnoreCase(command.trim())) {
                    cancelPendingActivity(state, "cancelled by operator");
                }
                stateStore.save(state);
                return;
            }
        }

        if (classification.type() == TerminalIntentType.HACKING_INTENT) {
            HackPolicyEngine.PolicyDecision decision = hackPolicyEngine.evaluate(command, List.copyOf(state.history()));
            if (!decision.allowed()) {
                state.setAiAvailabilityState(AiAvailabilityState.DEGRADED);
                appendLine(state, "hack failed: " + decision.reason());
                stateStore.save(state);
                return;
            }

            if (minigameChallengeService.shouldRequireChallenge(command)) {
                appendLine(state, "security challenge required (minigame hook ready)");
            }

            String workflowId = "wf-" + System.currentTimeMillis();
            state.setWorkflowState(new WorkflowState(workflowId, command, 0, level.getGameTime(), true));
            state.setActivity(TerminalActivity.RUNNING_WORKFLOW);
            state.setPendingUntilGameTime(level.getGameTime() + 20L * 90L);
            pendingWorkflowId = workflowId;
            pendingEvents.clear();
            pendingEventIndex = 0;
            nextEventGameTime = -1L;
            nextThinkingTick = level.getGameTime();
            thinkingFrame = 0;

            String context = "terminal=" + terminalId + ",dim=" + level.dimension().location();
            String history = String.join(" | ", state.history().stream().skip(Math.max(0, state.history().size() - 10)).toList());
            pendingAiFuture = CompletableFuture.supplyAsync(() -> aiHackService.execute(level, command, context, history));

            stateStore.save(state);
            return;
        }

        appendLine(state, "unknown input");
        stateStore.save(state);
    }

    public void tick(TerminalStateStore stateStore, ServerLevel level, String terminalId) {
        TerminalState state = stateStore.load();
        messageBus.register(level, terminalId);

        List<TerminalMessage> inbox = messageBus.drainInbox(level, terminalId);
        for (TerminalMessage msg : inbox) {
            String line = "[msg:" + msg.channel() + "] " + msg.fromTerminalId() + " -> " + msg.body();
            state.inbox().add(line);
            appendLine(state, line);
        }

        if (pendingAiFuture != null && pendingAiFuture.isDone() && state.workflowState() != null && state.workflowState().running()) {
            try {
                AiHackResult result = pendingAiFuture.getNow(null);
                if (result != null) {
                    state.setAiAvailabilityState(result.availabilityState());
                    appendLine(state, "[live:ai.think|");
                    List<String> aiLines = sequenceEngine.toTerminalLines(result, state.workflowState().commandText());
                    queueWorkflowEvents(aiLines, level, state);
                }
            } catch (Exception ignored) {
                state.setAiAvailabilityState(AiAvailabilityState.DEGRADED);
                appendLine(state, "[error] workflow failed; switching to degraded mode");
                completeWorkflow(state);
            } finally {
                pendingAiFuture = null;
                pendingWorkflowId = "";
            }
        }

        if (pendingAiFuture != null && !pendingAiFuture.isDone() && state.workflowState() != null && state.workflowState().running()) {
            if (nextThinkingTick < 0 || level.getGameTime() >= nextThinkingTick) {
                appendLine(state, "[live:ai.think|processing " + THINKING_FRAMES[thinkingFrame] + "]");
                thinkingFrame = (thinkingFrame + 1) % THINKING_FRAMES.length;
                nextThinkingTick = level.getGameTime() + 4L;
            }
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
                }
            }
        }

        if (state.workflowState() != null && state.workflowState().running()
                && state.pendingUntilGameTime() > 0
                && level.getGameTime() > state.pendingUntilGameTime()) {
            cancelPendingActivity(state, "workflow timed out");
            pendingAiFuture = null;
            pendingWorkflowId = "";
            pendingEvents.clear();
            pendingEventIndex = 0;
            nextEventGameTime = -1L;
            nextThinkingTick = -1L;
        }

        stateStore.save(state);
    }

    public void terminatePendingActivity(TerminalStateStore stateStore, String reason) {
        TerminalState state = stateStore.load();
        cancelPendingActivity(state, reason);
        stateStore.save(state);
        pendingAiFuture = null;
        pendingWorkflowId = "";
        pendingEvents.clear();
        pendingEventIndex = 0;
        nextEventGameTime = -1L;
        nextThinkingTick = -1L;
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
        state.setActiveOperatorUuid("");
        appendLine(state, "pending activity reset: " + reason);
        state.setActivity(TerminalActivity.IDLE);
    }

    private void queueWorkflowEvents(List<String> aiLines, ServerLevel level, TerminalState state) {
        pendingEvents.clear();
        pendingEventIndex = 0;
        nextEventGameTime = level.getGameTime() + 1L;
        nextThinkingTick = -1L;

        for (String raw : aiLines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String lower = line.toLowerCase();

            if (lower.startsWith("wait:")) {
                long ticks = msToTicks(parseIntSafe(line.substring(5).trim(), 500));
                pendingEvents.add(WorkflowEvent.waitOnly(ticks));
                continue;
            }
            if (lower.startsWith("mc:")) {
                String command = line.substring(3).trim();
                pendingEvents.add(WorkflowEvent.command(command, 10L));
                continue;
            }
            if (lower.startsWith("tts:")) {
                String spoken = line.substring(4).trim();
                pendingEvents.add(WorkflowEvent.voice(spoken, 10L));
                continue;
            }
            if (lower.startsWith("progress:")) {
                pendingEvents.addAll(parseProgressEvents(line.substring(9).trim()));
                continue;
            }
            if (lower.startsWith("spinner:")) {
                pendingEvents.addAll(parseSpinnerEvents(line.substring(8).trim()));
                continue;
            }
            if (lower.startsWith("tree:")) {
                pendingEvents.addAll(parseTreeEvents(line.substring(5).trim()));
                continue;
            }
            if (lower.startsWith("live:")) {
                String payload = line.substring(5).trim();
                String[] parts = payload.split("\\|", 2);
                String id = parts.length > 0 ? parts[0].trim() : "status";
                String text = parts.length > 1 ? parts[1].trim() : "";
                pendingEvents.add(WorkflowEvent.live(id, text, 8L));
                continue;
            }
            pendingEvents.add(WorkflowEvent.text(line, 10L));
        }

        if (pendingEvents.isEmpty()) {
            appendLine(state, "no actionable workflow lines returned");
            completeWorkflow(state);
        }
    }

    private List<WorkflowEvent> parseProgressEvents(String payload) {
        String[] parts = payload.split("\\|", 2);
        String label = parts.length > 0 ? parts[0].trim() : "loading";
        int seconds = parts.length > 1 ? parseIntSafe(parts[1].trim(), 5) : 5;
        seconds = Math.max(1, Math.min(seconds, 20));
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
        seconds = Math.max(1, Math.min(seconds, 20));
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
        depth = Math.max(1, Math.min(depth, 5));
        List<WorkflowEvent> events = new ArrayList<>();
        events.add(WorkflowEvent.text(root + "/", 8L));
        for (int d = 1; d <= depth; d++) {
            String indent = "  ".repeat(d - 1);
            events.add(WorkflowEvent.text(indent + "|-- kernel_" + d + ".bin", 8L));
            events.add(WorkflowEvent.text(indent + "|-- daemon_" + d + ".svc", 8L));
            events.add(WorkflowEvent.text(indent + "`-- ghost_proc_" + d, 8L));
        }
        return events;
    }

    private void applyWorkflowEvent(WorkflowEvent event, ServerLevel level, TerminalState state) {
        switch (event.type()) {
            case TEXT -> appendLine(state, event.payload());
            case VOICE -> {
                appendLine(state, "[voice] " + event.payload());
            }
            case COMMAND -> {
                executeMinecraftCommand(level, event.payload(), state);
            }
            case LIVE -> {
                appendLine(state, "[live:" + event.liveId() + "|" + event.payload());
            }
            case WAIT -> {
                // no-op step for temporal pacing
            }
        }
    }

    private void executeMinecraftCommand(ServerLevel level, String commandText, TerminalState state) {
        String command = sanitizeAiCommand(commandText);
        if (command.isEmpty()) {
            return;
        }
        try {
            CommandSourceStack source = level.getServer()
                    .createCommandSourceStack()
                    .withSuppressedOutput()
                    .withPermission(4);
            int result = level.getServer().getCommands().getDispatcher().execute(command, source);
            appendLine(state, "cmd> /" + command + " (" + result + ")");
        } catch (Exception e) {
            appendLine(state, "[error] command failed /" + command + ": " + e.getClass().getSimpleName());
        }
    }

    private String sanitizeAiCommand(String commandText) {
        String command = commandText == null ? "" : commandText.trim();
        if (command.isEmpty()) {
            return "";
        }
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        // LLMs often terminate shell-like lines with ';' which is invalid in MC commands.
        while (command.endsWith(";")) {
            command = command.substring(0, command.length() - 1).trim();
        }
        command = command
                .replace("minecraft:glitch_entity", "examplemod:glitch")
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2019', '\'')
                .replace('\u2018', '\'');
        return command;
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
