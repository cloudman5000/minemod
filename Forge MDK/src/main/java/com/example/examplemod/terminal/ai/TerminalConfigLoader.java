package com.example.examplemod.terminal.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TerminalConfigLoader {
    private static final Gson GSON = new Gson();
    private static final String FILE_NAME = "examplemod-terminal.json";

    private TerminalConfigLoader() {
    }

    public static TerminalAiConfig loadOrCreate(Path gameDir) {
        Path configDir = gameDir.resolve("config");
        Path filePath = configDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, templateJson(), StandardCharsets.UTF_8);
                return TerminalAiConfig.defaults();
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonObject openai = root.has("openai") ? root.getAsJsonObject("openai") : new JsonObject();

            String apiKey = openai.has("apiKey") ? openai.get("apiKey").getAsString() : "";
            String model = openai.has("model") ? openai.get("model").getAsString() : "gpt-5.3-mini";
            String endpoint = openai.has("endpoint")
                    ? openai.get("endpoint").getAsString()
                    : "https://api.openai.com/v1/chat/completions";
            int timeout = openai.has("timeoutSeconds") ? openai.get("timeoutSeconds").getAsInt() : 20;

            return new TerminalAiConfig(apiKey, model, endpoint, timeout);
        } catch (Exception ignored) {
            return TerminalAiConfig.defaults();
        }
    }

    private static String templateJson() {
        JsonObject root = new JsonObject();
        JsonObject openai = new JsonObject();
        openai.addProperty("apiKey", "");
        openai.addProperty("model", "gpt-5.3-mini");
        openai.addProperty("endpoint", "https://api.openai.com/v1/chat/completions");
        openai.addProperty("timeoutSeconds", 20);
        root.add("openai", openai);
        root.addProperty("notes", "Set openai.apiKey here or use OPENAI_API_KEY environment variable.");
        return GSON.toJson(root);
    }
}
