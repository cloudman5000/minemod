package com.example.examplemod.terminal.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconGateHackPolicyEngineTest {
    private final ReconGateHackPolicyEngine policy = new ReconGateHackPolicyEngine();

    @Test
    void blocksComplexHackWithoutRecon() {
        HackPolicyEngine.PolicyDecision decision = policy.evaluate("apply ultimate enhancements", List.of("status"));
        assertFalse(decision.allowed());
    }

    @Test
    void allowsComplexHackAfterRecon() {
        HackPolicyEngine.PolicyDecision decision = policy.evaluate(
                "apply ultimate enhancements",
                List.of("scan ingredients", "analyze network")
        );
        assertTrue(decision.allowed());
    }
}
