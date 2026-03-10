package com.example.examplemod.terminal.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TerminalGameplayConfigLoader {
    private static final String FILE_NAME = "examplemod-terminal.json";
    private static final String ROOT_KEY = "terminalOs";

    private TerminalGameplayConfigLoader() {
    }

    public static TerminalGameplayConfig load(Path gameDir) {
        Path filePath = gameDir.resolve("config").resolve(FILE_NAME);
        try {
            if (!Files.exists(filePath)) {
                return defaults();
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            if (!root.has(ROOT_KEY) || !root.get(ROOT_KEY).isJsonObject()) {
                return defaults();
            }
            return parse(root.getAsJsonObject(ROOT_KEY));
        } catch (Exception ignored) {
            return defaults();
        }
    }

    public static JsonObject defaultsAsJsonObject() {
        JsonObject os = new JsonObject();
        os.addProperty("aiFlavorEnabled", false);
        os.addProperty("rootNodeId", "root");

        JsonObject nodes = new JsonObject();
        nodes.add("root", node("root", "Mainframe OS", "select module", "",
                List.of(
                        entry("diagnostics", "Diagnostics Wing", "NODE", "diagnostics", 0, "", ""),
                        entry("games", "Arcade Cabinet", "NODE", "games", 0, "flag:diag_ready", ""),
                        entry("vault", "Prize Vault", "NODE", "vault", 0, "flag:arcade_unlocked", "")
                )));
        nodes.add("diagnostics", node("diagnostics", "Diagnostics Wing", "strange maintenance routines", "root",
                List.of(
                        entry("trace", "Run Ghost Trace", "APP", "ghost_trace", 1, "", "flag:diag_trace"),
                        entry("handshake", "Handshake Puzzle", "APP", "ritual_handshake", 1, "flag:diag_trace", "flag:diag_ready")
                )));
        nodes.add("games", node("games", "Arcade Cabinet", "retro simulations", "root",
                List.of(
                        entry("pong", "PONG_84", "MINIGAME", "pong84", 1, "flag:diag_ready", "flag:pong_complete"),
                        entry("invaders", "Space Panic", "MINIGAME", "spacepanic", 2, "flag:pong_complete", "flag:arcade_unlocked")
                )));
        nodes.add("vault", node("vault", "Prize Vault", "high-value fabrications", "root",
                List.of(
                        entry("diamonds", "Fabricate Diamonds", "REWARD", "diamonds_bundle", 2, "flag:pong_complete", ""),
                        entry("netherite", "Forge Netherite Kit", "REWARD", "netherite_kit", 4, "flag:arcade_unlocked", ""),
                        entry("pet", "Spawn Pet Friends", "REWARD", "pet_friends", 3, "flag:arcade_unlocked", ""),
                        entry("flight", "Temporary Flight Patch", "REWARD", "temp_flight", 3, "flag:arcade_unlocked", "")
                )));
        os.add("nodes", nodes);

        JsonObject apps = new JsonObject();
        apps.add("ghost_trace", script("ghost_trace", "Ghost Trace",
                List.of(
                        "SPINNER:calibrating cathode relays|2",
                        "TREE:/signals/ghostmesh|3",
                        "LIVE:diag|trace signature captured."
                ), "flag:diag_trace"));
        apps.add("ritual_handshake", script("ritual_handshake", "Ritual Handshake",
                List.of(
                        "SPINNER:negotiating unstable protocol|3",
                        "PROGRESS:handshake integrity|5",
                        "TREE:/bus/arcade/handshake|4",
                        "LIVE:status|arcade cabinet unlocked."
                ), "flag:diag_ready"));
        os.add("apps", apps);

        JsonObject rewards = new JsonObject();
        rewards.add("diamonds_bundle", reward("diamonds_bundle", "Diamonds", "small premium bundle", 2, 20,
                List.of("give @p minecraft:diamond 24"), "flag:pong_complete"));
        rewards.add("netherite_kit", reward("netherite_kit", "Netherite Kit", "late-game equipment drop", 5, 60,
                List.of(
                        "give @p minecraft:netherite_sword 1",
                        "give @p minecraft:netherite_pickaxe 1",
                        "give @p minecraft:netherite_chestplate 1"
                ), "flag:arcade_unlocked"));
        rewards.add("pet_friends", reward("pet_friends", "Pet Friends", "summon loyal entities", 3, 45,
                List.of(
                        "summon minecraft:wolf ~2 ~ ~ {Tame:1b,Owner:[I;0,0,0,0]}",
                        "summon minecraft:cat ~-2 ~ ~ {Tame:1b,Owner:[I;0,0,0,0]}"
                ), "flag:arcade_unlocked"));
        rewards.add("temp_flight", reward("temp_flight", "Temporary Flight", "creative-like boost", 4, 90,
                List.of(
                        "effect give @p minecraft:slow_falling 240 1 true",
                        "effect give @p minecraft:jump_boost 240 2 true"
                ), "flag:arcade_unlocked"));
        rewards.add("terraform_farm", reward("terraform_farm", "Terraform Farm", "builds crop field footprint", 6, 120,
                List.of(
                        "fill ~-12 ~-1 ~-12 ~12 ~-1 ~12 minecraft:farmland",
                        "fill ~-12 ~ ~-12 ~12 ~ ~12 minecraft:wheat[age=7]"
                ), "flag:arcade_unlocked"));
        os.add("rewards", rewards);

        JsonObject minigames = new JsonObject();
        minigames.add("pong84", minigame("pong84", "PONG_84", "pong", 14, 1, "diamonds_bundle", "flag:pong_complete"));
        minigames.add("spacepanic", minigame("spacepanic", "Space Panic", "invaders", 22, 2, "netherite_kit", "flag:arcade_unlocked"));
        os.add("minigames", minigames);

        JsonObject corruption = new JsonObject();
        JsonArray thresholds = new JsonArray();
        thresholds.add(6);
        thresholds.add(15);
        thresholds.add(30);
        thresholds.add(50);
        corruption.add("stageThresholds", thresholds);
        corruption.addProperty("ambientSpawnIntervalTicks", 240);
        corruption.addProperty("maxCorruption", 100);
        os.add("corruption", corruption);

        return os;
    }

    public static TerminalGameplayConfig defaults() {
        return parse(defaultsAsJsonObject());
    }

    private static TerminalGameplayConfig parse(JsonObject os) {
        boolean aiFlavorEnabled = getBoolean(os, "aiFlavorEnabled", false);
        String rootNodeId = getString(os, "rootNodeId", "root");

        Map<String, TerminalMenuNode> nodes = new LinkedHashMap<>();
        JsonObject nodesObj = getObject(os, "nodes");
        for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject nodeObj = entry.getValue().getAsJsonObject();
            String id = getString(nodeObj, "id", entry.getKey());
            String title = getString(nodeObj, "title", id);
            String description = getString(nodeObj, "description", "");
            String parentNodeId = getString(nodeObj, "parentNodeId", "");
            List<TerminalMenuEntry> entries = new ArrayList<>();
            JsonArray entriesArray = getArray(nodeObj, "entries");
            for (JsonElement e : entriesArray) {
                if (!e.isJsonObject()) {
                    continue;
                }
                JsonObject eo = e.getAsJsonObject();
                String entryId = getString(eo, "id", "entry_" + entries.size());
                String label = getString(eo, "label", entryId);
                TerminalMenuEntryType type = parseEntryType(getString(eo, "type", "APP"));
                String target = getString(eo, "target", "");
                int cost = Math.max(0, getInt(eo, "corruptionCost", 0));
                String requiredFlag = getString(eo, "requiredFlag", "");
                String grantsFlag = getString(eo, "grantsFlag", "");
                entries.add(new TerminalMenuEntry(entryId, label, type, target, cost, requiredFlag, grantsFlag));
            }
            nodes.put(id, new TerminalMenuNode(id, title, description, parentNodeId, entries));
        }

        Map<String, TerminalScriptDefinition> apps = new LinkedHashMap<>();
        JsonObject appsObj = getObject(os, "apps");
        for (Map.Entry<String, JsonElement> entry : appsObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject ao = entry.getValue().getAsJsonObject();
            String id = getString(ao, "id", entry.getKey());
            String title = getString(ao, "title", id);
            List<String> lines = new ArrayList<>();
            for (JsonElement line : getArray(ao, "lines")) {
                if (line.isJsonPrimitive()) {
                    lines.add(line.getAsString());
                }
            }
            String grantsFlag = getString(ao, "grantsFlag", "");
            apps.put(id, new TerminalScriptDefinition(id, title, lines, grantsFlag));
        }

        Map<String, TerminalRewardDefinition> rewards = new LinkedHashMap<>();
        JsonObject rewardsObj = getObject(os, "rewards");
        for (Map.Entry<String, JsonElement> entry : rewardsObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject ro = entry.getValue().getAsJsonObject();
            String id = getString(ro, "id", entry.getKey());
            String title = getString(ro, "title", id);
            String description = getString(ro, "description", "");
            int corruptionCost = Math.max(0, getInt(ro, "corruptionCost", 1));
            int cooldownSeconds = Math.max(1, getInt(ro, "cooldownSeconds", 30));
            List<String> commands = new ArrayList<>();
            for (JsonElement line : getArray(ro, "commands")) {
                if (line.isJsonPrimitive()) {
                    commands.add(line.getAsString());
                }
            }
            String requiredFlag = getString(ro, "requiredFlag", "");
            rewards.put(id, new TerminalRewardDefinition(id, title, description, corruptionCost, cooldownSeconds, commands, requiredFlag));
        }

        Map<String, TerminalMinigameDefinition> minigames = new LinkedHashMap<>();
        JsonObject minigamesObj = getObject(os, "minigames");
        for (Map.Entry<String, JsonElement> entry : minigamesObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject mo = entry.getValue().getAsJsonObject();
            String id = getString(mo, "id", entry.getKey());
            String title = getString(mo, "title", id);
            String kind = getString(mo, "kind", "pong");
            int targetScore = Math.max(5, getInt(mo, "targetScore", 10));
            int corruptionCost = Math.max(0, getInt(mo, "corruptionCost", 1));
            String rewardId = getString(mo, "rewardId", "");
            String grantsFlag = getString(mo, "grantsFlag", "");
            minigames.put(id, new TerminalMinigameDefinition(id, title, kind, targetScore, corruptionCost, rewardId, grantsFlag));
        }

        JsonObject corruptionObj = getObject(os, "corruption");
        List<Integer> thresholds = new ArrayList<>();
        for (JsonElement e : getArray(corruptionObj, "stageThresholds")) {
            if (e.isJsonPrimitive()) {
                thresholds.add(Math.max(1, e.getAsInt()));
            }
        }
        if (thresholds.isEmpty()) {
            thresholds = List.of(6, 15, 30, 50);
        }
        int ambientSpawnIntervalTicks = Math.max(40, getInt(corruptionObj, "ambientSpawnIntervalTicks", 240));
        int maxCorruption = Math.max(20, getInt(corruptionObj, "maxCorruption", 100));

        return new TerminalGameplayConfig(
                aiFlavorEnabled,
                rootNodeId,
                nodes,
                apps,
                rewards,
                minigames,
                new TerminalCorruptionConfig(thresholds, ambientSpawnIntervalTicks, maxCorruption)
        );
    }

    private static JsonObject node(String id, String title, String description, String parentNodeId, List<JsonObject> entries) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("title", title);
        obj.addProperty("description", description);
        obj.addProperty("parentNodeId", parentNodeId);
        JsonArray array = new JsonArray();
        for (JsonObject e : entries) {
            array.add(e);
        }
        obj.add("entries", array);
        return obj;
    }

    private static JsonObject entry(String id, String label, String type, String target, int corruptionCost, String requiredFlag, String grantsFlag) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("label", label);
        obj.addProperty("type", type);
        obj.addProperty("target", target);
        obj.addProperty("corruptionCost", corruptionCost);
        if (requiredFlag != null && !requiredFlag.isBlank()) {
            obj.addProperty("requiredFlag", requiredFlag);
        }
        if (grantsFlag != null && !grantsFlag.isBlank()) {
            obj.addProperty("grantsFlag", grantsFlag);
        }
        return obj;
    }

    private static JsonObject script(String id, String title, List<String> lines, String grantsFlag) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("title", title);
        JsonArray array = new JsonArray();
        for (String line : lines) {
            array.add(line);
        }
        obj.add("lines", array);
        if (grantsFlag != null && !grantsFlag.isBlank()) {
            obj.addProperty("grantsFlag", grantsFlag);
        }
        return obj;
    }

    private static JsonObject reward(String id, String title, String description, int corruptionCost, int cooldownSeconds, List<String> commands, String requiredFlag) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("title", title);
        obj.addProperty("description", description);
        obj.addProperty("corruptionCost", corruptionCost);
        obj.addProperty("cooldownSeconds", cooldownSeconds);
        JsonArray array = new JsonArray();
        for (String command : commands) {
            array.add(command);
        }
        obj.add("commands", array);
        if (requiredFlag != null && !requiredFlag.isBlank()) {
            obj.addProperty("requiredFlag", requiredFlag);
        }
        return obj;
    }

    private static JsonObject minigame(String id, String title, String kind, int targetScore, int corruptionCost, String rewardId, String grantsFlag) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("title", title);
        obj.addProperty("kind", kind);
        obj.addProperty("targetScore", targetScore);
        obj.addProperty("corruptionCost", corruptionCost);
        obj.addProperty("rewardId", rewardId);
        if (grantsFlag != null && !grantsFlag.isBlank()) {
            obj.addProperty("grantsFlag", grantsFlag);
        }
        return obj;
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonObject()) {
            return obj.getAsJsonObject(key);
        }
        return new JsonObject();
    }

    private static JsonArray getArray(JsonObject obj, String key) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonArray()) {
            return obj.getAsJsonArray(key);
        }
        return new JsonArray();
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive()) {
            return obj.get(key).getAsString();
        }
        return fallback;
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        try {
            if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        try {
            if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static TerminalMenuEntryType parseEntryType(String raw) {
        try {
            return TerminalMenuEntryType.valueOf(raw.toUpperCase());
        } catch (Exception ignored) {
            return TerminalMenuEntryType.APP;
        }
    }
}
