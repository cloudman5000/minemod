package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.config.TerminalCorruptionConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultTerminalCorruptionServiceTest {
    @Test
    void applyUsageAdvancesStageBasedOnThresholds() {
        DefaultTerminalCorruptionService service = new DefaultTerminalCorruptionService();
        TerminalCorruptionConfig config = new TerminalCorruptionConfig(List.of(5, 10, 20), 200, 100);

        TerminalCorruptionService.CorruptionUpdate update = service.applyUsage(null, null, 8, 5, config);
        assertTrue(update.points() >= 13);
        assertTrue(update.stage() >= 2);
        assertFalse(update.lines().stream().anyMatch(line -> line.toLowerCase().contains("corruption")));
    }
}
