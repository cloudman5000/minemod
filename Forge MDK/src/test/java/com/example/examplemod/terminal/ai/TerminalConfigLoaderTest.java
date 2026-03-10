package com.example.examplemod.terminal.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadOrCreateCreatesTemplateWithDefaults() {
        TerminalAiConfig config = TerminalConfigLoader.loadOrCreate(tempDir);
        Path configFile = tempDir.resolve("config").resolve("examplemod-terminal.json");

        assertTrue(Files.exists(configFile));
        assertEquals("gpt-5.3-mini", config.model());
        assertEquals("", config.apiKey());
        assertEquals(20, config.timeoutSeconds());
    }

    @Test
    void loadOrCreateParsesExistingConfigValues() throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("examplemod-terminal.json");
        Files.writeString(configFile, """
                {
                  "openai": {
                    "apiKey": "abc123",
                    "model": "gpt-5.3-mini",
                    "endpoint": "https://example.test/v1/chat/completions",
                    "timeoutSeconds": 42
                  }
                }
                """, StandardCharsets.UTF_8);

        TerminalAiConfig config = TerminalConfigLoader.loadOrCreate(tempDir);
        assertEquals("abc123", config.apiKey());
        assertEquals("https://example.test/v1/chat/completions", config.endpoint());
        assertEquals(42, config.timeoutSeconds());
    }

    @Test
    void loadOrCreateFallsBackToDefaultsOnInvalidJson() throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("examplemod-terminal.json");
        Files.writeString(configFile, "{ this is invalid json", StandardCharsets.UTF_8);

        TerminalAiConfig config = TerminalConfigLoader.loadOrCreate(tempDir);
        assertEquals("", config.apiKey());
        assertEquals("gpt-5.3-mini", config.model());
    }
}
