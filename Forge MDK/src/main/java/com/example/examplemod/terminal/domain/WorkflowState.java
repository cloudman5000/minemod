package com.example.examplemod.terminal.domain;

import net.minecraft.nbt.CompoundTag;

public final class WorkflowState {
    private final String workflowId;
    private final String commandText;
    private final String contextId;
    private int stepIndex;
    private long startedAtGameTime;
    private boolean running;

    public WorkflowState(String workflowId, String commandText, int stepIndex, long startedAtGameTime, boolean running) {
        this(workflowId, commandText, "", stepIndex, startedAtGameTime, running);
    }

    public WorkflowState(String workflowId, String commandText, String contextId, int stepIndex, long startedAtGameTime, boolean running) {
        this.workflowId = workflowId;
        this.commandText = commandText;
        this.contextId = contextId;
        this.stepIndex = stepIndex;
        this.startedAtGameTime = startedAtGameTime;
        this.running = running;
    }

    public String workflowId() {
        return workflowId;
    }

    public String commandText() {
        return commandText;
    }

    public String contextId() {
        return contextId;
    }

    public int stepIndex() {
        return stepIndex;
    }

    public long startedAtGameTime() {
        return startedAtGameTime;
    }

    public boolean running() {
        return running;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("workflowId", workflowId);
        tag.putString("commandText", commandText);
        tag.putString("contextId", contextId);
        tag.putInt("stepIndex", stepIndex);
        tag.putLong("startedAtGameTime", startedAtGameTime);
        tag.putBoolean("running", running);
        return tag;
    }

    public static WorkflowState fromTag(CompoundTag tag) {
        return new WorkflowState(
                tag.getString("workflowId"),
                tag.getString("commandText"),
                tag.getString("contextId"),
                tag.getInt("stepIndex"),
                tag.getLong("startedAtGameTime"),
                tag.getBoolean("running")
        );
    }
}
