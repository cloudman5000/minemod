package com.example.examplemod.terminal.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TerminalState {
    private TerminalActivity activity = TerminalActivity.IDLE;
    private AiAvailabilityState aiAvailabilityState = AiAvailabilityState.DISABLED;
    private final List<String> history = new ArrayList<>();
    private final List<String> inbox = new ArrayList<>();
    private WorkflowState workflowState;
    private String activeOperatorUuid = "";
    private long pendingUntilGameTime = -1L;
    private String activeNodeId = "root";
    private int selectedIndex = 0;
    private String activeMinigameId = "";
    private String activeMinigameKind = "";
    private String minigameStateJson = "";
    private int arcadeMoveIntent = 0;
    private boolean arcadeFireQueued = false;
    private int corruptionPoints = 0;
    private int corruptionStage = 0;
    private final Set<String> progressionFlags = new HashSet<>();

    public TerminalActivity activity() {
        return activity;
    }

    public void setActivity(TerminalActivity activity) {
        this.activity = activity;
    }

    public AiAvailabilityState aiAvailabilityState() {
        return aiAvailabilityState;
    }

    public void setAiAvailabilityState(AiAvailabilityState aiAvailabilityState) {
        this.aiAvailabilityState = aiAvailabilityState;
    }

    public List<String> history() {
        return history;
    }

    public List<String> inbox() {
        return inbox;
    }

    public WorkflowState workflowState() {
        return workflowState;
    }

    public void setWorkflowState(WorkflowState workflowState) {
        this.workflowState = workflowState;
    }

    public String activeOperatorUuid() {
        return activeOperatorUuid;
    }

    public void setActiveOperatorUuid(String activeOperatorUuid) {
        this.activeOperatorUuid = activeOperatorUuid;
    }

    public long pendingUntilGameTime() {
        return pendingUntilGameTime;
    }

    public void setPendingUntilGameTime(long pendingUntilGameTime) {
        this.pendingUntilGameTime = pendingUntilGameTime;
    }

    public String activeNodeId() {
        return activeNodeId;
    }

    public void setActiveNodeId(String activeNodeId) {
        this.activeNodeId = activeNodeId;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public String activeMinigameId() {
        return activeMinigameId;
    }

    public void setActiveMinigameId(String activeMinigameId) {
        this.activeMinigameId = activeMinigameId;
    }

    public String activeMinigameKind() {
        return activeMinigameKind;
    }

    public void setActiveMinigameKind(String activeMinigameKind) {
        this.activeMinigameKind = activeMinigameKind;
    }

    public String minigameStateJson() {
        return minigameStateJson;
    }

    public void setMinigameStateJson(String minigameStateJson) {
        this.minigameStateJson = minigameStateJson;
    }

    public int arcadeMoveIntent() {
        return arcadeMoveIntent;
    }

    public void setArcadeMoveIntent(int arcadeMoveIntent) {
        this.arcadeMoveIntent = Math.max(-1, Math.min(1, arcadeMoveIntent));
    }

    public boolean arcadeFireQueued() {
        return arcadeFireQueued;
    }

    public void setArcadeFireQueued(boolean arcadeFireQueued) {
        this.arcadeFireQueued = arcadeFireQueued;
    }

    public int corruptionPoints() {
        return corruptionPoints;
    }

    public void setCorruptionPoints(int corruptionPoints) {
        this.corruptionPoints = corruptionPoints;
    }

    public int corruptionStage() {
        return corruptionStage;
    }

    public void setCorruptionStage(int corruptionStage) {
        this.corruptionStage = corruptionStage;
    }

    public Set<String> progressionFlags() {
        return progressionFlags;
    }
}
