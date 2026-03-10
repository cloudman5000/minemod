package com.example.examplemod.terminal.ai;

import com.example.examplemod.terminal.domain.AiAvailabilityState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFallbackAiHackServiceTest {
    @Test
    void fallbackAlwaysReturnsDegradedAndHelpfulLines() {
        LocalFallbackAiHackService service = new LocalFallbackAiHackService();

        AiHackResult result = service.execute(null, "hack flavor matrix", "ctx", "history");

        assertEquals(AiAvailabilityState.DEGRADED, result.availabilityState());
        assertTrue(result.outputLines().getFirst().contains("DEGRADED MODE"));
        assertTrue(result.outputLines().stream().anyMatch(line -> line.contains("limited local parser engaged")));
    }
}
