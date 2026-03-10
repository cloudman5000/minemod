package com.example.examplemod.terminal.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalGameplayConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsContainExpectedBigBangContent() {
        TerminalGameplayConfig cfg = TerminalGameplayConfigLoader.defaults();
        assertEquals("root", cfg.rootNodeId());
        assertTrue(cfg.nodes().containsKey("games"));
        assertTrue(cfg.minigames().containsKey("pong84"));
        assertTrue(cfg.rewards().containsKey("netherite_kit"));
        assertFalse(cfg.aiFlavorEnabled());
    }

    @Test
    void loadParsesTerminalOsFromConfigFile() throws IOException {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("examplemod-terminal.json");
        Files.writeString(configFile, """
                {
                  "openai": {"apiKey":"", "model":"gpt-4o-mini"},
                  "terminalOs": {
                    "aiFlavorEnabled": true,
                    "rootNodeId": "root",
                    "nodes": {
                      "root": {
                        "id": "root",
                        "title": "Root",
                        "description": "desc",
                        "parentNodeId": "",
                        "entries": [
                          {"id":"g", "label":"Games", "type":"MINIGAME", "target":"pong84", "corruptionCost":2, "requiredFlag":"flag:diag_ready", "grantsFlag":"flag:pong_complete"}
                        ]
                      }
                    },
                    "apps": {},
                    "rewards": {},
                    "minigames": {
                      "pong84": {
                        "id":"pong84",
                        "title":"PONG",
                        "kind":"pong",
                        "targetScore":12,
                        "corruptionCost":1,
                        "rewardId":"",
                        "grantsFlag":"flag:pong_complete"
                      }
                    },
                    "corruption": {
                      "stageThresholds": [5,10,20],
                      "ambientSpawnIntervalTicks": 180,
                      "maxCorruption": 80
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        TerminalGameplayConfig cfg = TerminalGameplayConfigLoader.load(tempDir);
        assertTrue(cfg.aiFlavorEnabled());
        assertEquals(1, cfg.nodes().get("root").entries().size());
        assertEquals(12, cfg.minigames().get("pong84").targetScore());
        assertEquals("flag:diag_ready", cfg.nodes().get("root").entries().getFirst().requiredFlag());
        assertEquals("flag:pong_complete", cfg.minigames().get("pong84").grantsFlag());
        assertEquals(80, cfg.corruption().maxCorruption());
    }
}
