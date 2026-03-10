package com.example.examplemod.terminal.ai;

import com.example.examplemod.terminal.domain.AiAvailabilityState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerLevel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class OpenAiHackService implements AiHackService {
    private static final String CHAT_COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";
    private static final String MODEL_FALLBACK = "gpt-4o-mini";

    private final CredentialResolver credentialResolver = new CredentialResolver();
    private final LocalFallbackAiHackService fallbackAiHackService = new LocalFallbackAiHackService();
    private final AiPromptComposer promptComposer = new DefaultAiPromptComposer();
    private final AiResponseValidator responseValidator = new SimpleAiResponseValidator();

    @Override
    public AiHackResult execute(ServerLevel level, String command, String terminalContext, String commandHistorySummary) {
        TerminalAiConfig cfg = credentialResolver.resolve(level);
        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            AiHackResult fallback = fallbackAiHackService.execute(level, command, terminalContext, commandHistorySummary);
            List<String> lines = new ArrayList<>(fallback.outputLines());
            lines.add("[error] openai api key missing: set OPENAI_API_KEY or config/examplemod-terminal.json");
            return new AiHackResult(lines, AiAvailabilityState.DEGRADED);
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(cfg.timeoutSeconds()))
                .build();
        String prompt = promptComposer.compose(command, terminalContext, commandHistorySummary);

        List<String> models = buildModelCandidates(cfg.model());
        String endpoint = cfg.endpoint() == null || cfg.endpoint().isBlank() ? CHAT_COMPLETIONS_ENDPOINT : cfg.endpoint();
        String lastError = "unavailable";

        for (String model : models) {
            AttemptResult chatAttempt = tryChatCompletions(client, cfg, endpoint, model, prompt);
            if (chatAttempt.success()) {
                return new AiHackResult(chatAttempt.lines(), AiAvailabilityState.ONLINE);
            }
            if (!chatAttempt.error().isBlank()) {
                lastError = chatAttempt.error();
            }

            AttemptResult responsesAttempt = tryResponsesApi(client, cfg, model, prompt);
            if (responsesAttempt.success()) {
                return new AiHackResult(responsesAttempt.lines(), AiAvailabilityState.ONLINE);
            }
            if (!responsesAttempt.error().isBlank()) {
                lastError = responsesAttempt.error();
            }
        }

        return degradedWithReason(level, command, terminalContext, commandHistorySummary, lastError);
    }

    static List<String> normalizeLines(String rawText) {
        if (rawText == null) {
            return List.of();
        }
        return rawText.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .limit(20)
                .toList();
    }

    private AttemptResult tryChatCompletions(HttpClient client, TerminalAiConfig cfg, String endpoint, String model, String prompt) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);

            JsonArray messages = new JsonArray();
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", """
                    You are a horror hacking terminal AI that creates immersive, creative, technical flows.
                    Build dramatic terminal sequences with pacing, evolving status, and "system" flavor.
                    Return 6-16 lines.
                    Use directives to drive interactivity:
                    - MC: <minecraft command>      execute command
                    - WAIT: <milliseconds>         pacing delay
                    - PROGRESS: <label>|<seconds> single-line progress bar + percentage
                    - SPINNER: <label>|<seconds>  single-line animated spinner
                    - TREE: <root>|<depth>        render a staged tree view over time
                    - LIVE: <id>|<text>           update one existing line in place
                    - TTS: <spoken text>          speak and also display text
                    Make scenarios fun, ominous, and varied (scans, overlays, pseudo-network traces, staged reveals).
                    Command rules:
                    - Do not use trailing semicolons.
                    - Use valid vanilla syntax only.
                    - For the custom entity, use examplemod:glitch (not minecraft:glitch_entity).
                    Do NOT include fake prompts like root@... and do NOT explain the directives.
                    """);
            messages.add(system);

            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", prompt);
            messages.add(user);

            body.add("messages", messages);
            body.addProperty("max_tokens", 220);
            body.addProperty("temperature", 0.7);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(cfg.timeoutSeconds()))
                    .header("Authorization", "Bearer " + cfg.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                return AttemptResult.failure(extractApiError(response.body(), response.statusCode()));
            }

            JsonObject parsed = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray choices = parsed.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return AttemptResult.failure("empty choices from chat completion");
            }

            String text = choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();

            List<String> lines = normalizeLines(text);
            if (!responseValidator.isValid(lines)) {
                return AttemptResult.failure("invalid chat response format");
            }
            return AttemptResult.success(lines);
        } catch (Exception e) {
            return AttemptResult.failure("chat completion exception: " + e.getClass().getSimpleName());
        }
    }

    private AttemptResult tryResponsesApi(HttpClient client, TerminalAiConfig cfg, String model, String prompt) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("input", """
                    System:
                    You are a horror hacking terminal AI that creates immersive, creative, technical flows.
                    Build dramatic terminal sequences with pacing, evolving status, and "system" flavor.
                    Return 6-16 lines.
                    Use directives to drive interactivity:
                    - MC: <minecraft command>      execute command
                    - WAIT: <milliseconds>         pacing delay
                    - PROGRESS: <label>|<seconds> single-line progress bar + percentage
                    - SPINNER: <label>|<seconds>  single-line animated spinner
                    - TREE: <root>|<depth>        render a staged tree view over time
                    - LIVE: <id>|<text>           update one existing line in place
                    - TTS: <spoken text>          speak and also display text
                    Make scenarios fun, ominous, and varied (scans, overlays, pseudo-network traces, staged reveals).
                    Command rules:
                    - Do not use trailing semicolons.
                    - Use valid vanilla syntax only.
                    - For the custom entity, use examplemod:glitch (not minecraft:glitch_entity).
                    Do NOT include fake prompts like root@... and do NOT explain the directives.

                    User:
                    """ + prompt);
            body.addProperty("max_output_tokens", 220);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESPONSES_ENDPOINT))
                    .timeout(Duration.ofSeconds(cfg.timeoutSeconds()))
                    .header("Authorization", "Bearer " + cfg.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                return AttemptResult.failure(extractApiError(response.body(), response.statusCode()));
            }

            JsonObject parsed = JsonParser.parseString(response.body()).getAsJsonObject();
            String text = extractResponseText(parsed);
            if (text.isBlank()) {
                return AttemptResult.failure("empty text from responses api");
            }
            List<String> lines = normalizeLines(text);
            if (!responseValidator.isValid(lines)) {
                return AttemptResult.failure("invalid responses api format");
            }
            return AttemptResult.success(lines);
        } catch (Exception e) {
            return AttemptResult.failure("responses api exception: " + e.getClass().getSimpleName());
        }
    }

    private String extractResponseText(JsonObject parsed) {
        if (parsed.has("output_text") && parsed.get("output_text").isJsonPrimitive()) {
            return parsed.get("output_text").getAsString();
        }
        if (!parsed.has("output") || !parsed.get("output").isJsonArray()) {
            return "";
        }

        StringBuilder joined = new StringBuilder();
        JsonArray output = parsed.getAsJsonArray("output");
        for (JsonElement outputItem : output) {
            if (!outputItem.isJsonObject()) {
                continue;
            }
            JsonObject outputObj = outputItem.getAsJsonObject();
            if (!outputObj.has("content") || !outputObj.get("content").isJsonArray()) {
                continue;
            }
            JsonArray content = outputObj.getAsJsonArray("content");
            for (JsonElement contentItem : content) {
                if (!contentItem.isJsonObject()) {
                    continue;
                }
                JsonObject contentObj = contentItem.getAsJsonObject();
                if (contentObj.has("text") && contentObj.get("text").isJsonPrimitive()) {
                    if (!joined.isEmpty()) {
                        joined.append('\n');
                    }
                    joined.append(contentObj.get("text").getAsString());
                }
            }
        }
        return joined.toString();
    }

    private static String extractApiError(String body, int statusCode) {
        try {
            JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
            if (parsed.has("error") && parsed.get("error").isJsonObject()) {
                JsonObject err = parsed.getAsJsonObject("error");
                if (err.has("message")) {
                    return "http " + statusCode + ": " + err.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "http " + statusCode;
    }

    private List<String> buildModelCandidates(String configuredModel) {
        String primary = (configuredModel == null || configuredModel.isBlank()) ? MODEL_FALLBACK : configuredModel.trim();
        if (MODEL_FALLBACK.equals(primary)) {
            return List.of(primary);
        }
        return List.of(primary, MODEL_FALLBACK);
    }

    private AiHackResult degradedWithReason(
            ServerLevel level,
            String command,
            String terminalContext,
            String commandHistorySummary,
            String reason
    ) {
        AiHackResult fallback = fallbackAiHackService.execute(level, command, terminalContext, commandHistorySummary);
        List<String> lines = new ArrayList<>(fallback.outputLines());
        String sanitized = reason == null ? "unknown" : reason.replaceAll("\\s+", " ").trim();
        if (sanitized.length() > 140) {
            sanitized = sanitized.substring(0, 140);
        }
        lines.add("[error] ai connect issue: " + sanitized);
        return new AiHackResult(lines, AiAvailabilityState.DEGRADED);
    }

    private record AttemptResult(boolean success, List<String> lines, String error) {
        static AttemptResult success(List<String> lines) {
            return new AttemptResult(true, lines, "");
        }

        static AttemptResult failure(String error) {
            return new AttemptResult(false, List.of(), error == null ? "" : error);
        }
    }
}
