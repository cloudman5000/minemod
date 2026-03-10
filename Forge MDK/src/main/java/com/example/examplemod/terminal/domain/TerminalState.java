package com.example.examplemod.terminal.domain;

import java.util.ArrayList;
import java.util.List;

public final class TerminalState {
    private TerminalActivity activity = TerminalActivity.IDLE;
    private AiAvailabilityState aiAvailabilityState = AiAvailabilityState.DISABLED;
    private final List<String> history = new ArrayList<>();
    private final List<String> inbox = new ArrayList<>();
    private WorkflowState workflowState;
    private String activeOperatorUuid = "";
    private long pendingUntilGameTime = -1L;

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
}
