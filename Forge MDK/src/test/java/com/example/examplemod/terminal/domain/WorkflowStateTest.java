package com.example.examplemod.terminal.domain;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowStateTest {
    @Test
    void workflowStateRoundTripsThroughNbt() {
        WorkflowState state = new WorkflowState("wf-1", "scan ingredients", 2, 12345L, true);
        CompoundTag tag = state.toTag();
        WorkflowState restored = WorkflowState.fromTag(tag);

        assertEquals("wf-1", restored.workflowId());
        assertEquals("scan ingredients", restored.commandText());
        assertEquals(2, restored.stepIndex());
        assertEquals(12345L, restored.startedAtGameTime());
        assertTrue(restored.running());
    }
}
