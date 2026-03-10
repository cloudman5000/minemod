package com.example.examplemod.terminal.intent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminalIntentRouterTest {
    private final TerminalIntentRouter router = new TerminalIntentRouter();

    @Test
    void classifiesHardcodedCommand() {
        IntentClassificationResult result = router.classify("status");
        assertEquals(TerminalIntentType.HARDCODED_COMMAND, result.type());
    }

    @Test
    void classifiesWorkflowControlCommand() {
        IntentClassificationResult result = router.classify("cancel");
        assertEquals(TerminalIntentType.WORKFLOW_CONTROL, result.type());
    }

    @Test
    void classifiesHackingCommandWithoutAiPrefix() {
        IntentClassificationResult result = router.classify("scan network and exploit weak point");
        assertEquals(TerminalIntentType.HACKING_INTENT, result.type());
    }
}
