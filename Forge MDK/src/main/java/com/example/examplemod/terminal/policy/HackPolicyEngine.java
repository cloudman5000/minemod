package com.example.examplemod.terminal.policy;

import java.util.List;

public interface HackPolicyEngine {
    PolicyDecision evaluate(String command, List<String> history);

    record PolicyDecision(boolean allowed, String reason) {
    }
}
