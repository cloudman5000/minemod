package com.example.examplemod.terminal.ai;

import com.example.examplemod.terminal.domain.AiAvailabilityState;
import com.example.examplemod.terminal.sequence.SimpleTerminalSequenceEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiUtilitiesTest {
    @Test
    void promptComposerIncludesCommandAndContext() {
        DefaultAiPromptComposer composer = new DefaultAiPromptComposer();
        String prompt = composer.compose("scan network", "term=abc", "history");

        assertTrue(prompt.contains("scan network"));
        assertTrue(prompt.contains("term=abc"));
    }

    @Test
    void responseValidatorRejectsInvalidLineSets() {
        SimpleAiResponseValidator validator = new SimpleAiResponseValidator();
        assertFalse(validator.isValid(List.of()));
        assertFalse(validator.isValid(null));
        assertTrue(validator.isValid(List.of("ok", "line2")));
    }

    @Test
    void sequenceEnginePrefixesHackingExecutionLine() {
        SimpleTerminalSequenceEngine engine = new SimpleTerminalSequenceEngine();
        AiHackResult result = new AiHackResult(List.of("line a", "line b"), AiAvailabilityState.ONLINE);
        List<String> lines = engine.toTerminalLines(result, "scan");

        assertEquals("line a", lines.getFirst());
        assertTrue(lines.contains("line a"));
    }

    @Test
    void openAiLineNormalizationKeepsDirectiveLines() {
        List<String> lines = OpenAiHackService.normalizeLines("""
                step one complete
                TTS: operator, reality anchor stable
                continue probe
                """);

        assertEquals("TTS: operator, reality anchor stable", lines.get(1));
    }
}
