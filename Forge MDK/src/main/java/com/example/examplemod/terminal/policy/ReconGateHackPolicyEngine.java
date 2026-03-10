package com.example.examplemod.terminal.policy;

import java.util.List;
import java.util.Set;

public final class ReconGateHackPolicyEngine implements HackPolicyEngine {
    private static final Set<String> COMPLEX_MARKERS = Set.of("ultimate", "maximum", "deep", "everything", "overdrive");
    private static final Set<String> RECON_MARKERS = Set.of("scan", "analyze", "list", "show", "detect", "probe");

    @Override
    public PolicyDecision evaluate(String command, List<String> history) {
        String normalized = command.toLowerCase();
        boolean complex = COMPLEX_MARKERS.stream().anyMatch(normalized::contains);
        if (!complex) {
            return new PolicyDecision(true, "simple_hack_allowed");
        }

        boolean hasRecon = history.stream()
                .map(String::toLowerCase)
                .anyMatch(line -> RECON_MARKERS.stream().anyMatch(line::contains));
        if (hasRecon) {
            return new PolicyDecision(true, "recon_satisfied");
        }
        return new PolicyDecision(false, "run reconnaissance first (scan/analyze/list)");
    }
}
