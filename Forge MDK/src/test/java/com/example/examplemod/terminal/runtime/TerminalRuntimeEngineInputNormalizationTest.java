package com.example.examplemod.terminal.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminalRuntimeEngineInputNormalizationTest {
    @Test
    void stripsWrappingDoubleQuotesFromSubmittedInput() {
        assertEquals("help", TerminalRuntimeEngine.normalizeSubmittedInput("  \"help\"  "));
        assertEquals("menu", TerminalRuntimeEngine.normalizeSubmittedInput("\"menu\""));
    }

    @Test
    void stripsWrappingSingleQuotesFromSubmittedInput() {
        assertEquals("ls", TerminalRuntimeEngine.normalizeSubmittedInput("'ls'"));
    }

    @Test
    void keepsInputWhenQuotesAreUnbalanced() {
        assertEquals("\"menu", TerminalRuntimeEngine.normalizeSubmittedInput("\"menu"));
        assertEquals("menu\"", TerminalRuntimeEngine.normalizeSubmittedInput("menu\""));
    }

    @Test
    void keepsInputWithoutWrappingQuotes() {
        assertEquals("open diagnostics", TerminalRuntimeEngine.normalizeSubmittedInput(" open diagnostics "));
    }
}
