package com.example.examplemod.client.audio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RetroTtsService {
    private static final String CONFIG_FILE = "examplemod-terminal.json";
    private static final String DEFAULT_TTS_MODEL = "gpt-4o-mini-tts";
    private static final String FALLBACK_TTS_MODEL = "gpt-4o-audio-preview";
    private static final String DEFAULT_TTS_ENDPOINT = "https://api.openai.com/v1/audio/speech";
    private static final int MAX_TEXT_LEN = 260;
    private static final RetroTtsService INSTANCE = new RetroTtsService();

    private final ExecutorService ttsExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "examplemod-retro-tts");
        t.setDaemon(true);
        return t;
    });
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final Random random = new Random();

    private long lastSpokenAtMs = 0L;

    public static RetroTtsService get() {
        return INSTANCE;
    }

    private RetroTtsService() {
    }

    public void speak(String rawText) {
        String text = sanitize(rawText);
        if (text.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSpokenAtMs < 250L) {
            return;
        }
        lastSpokenAtMs = now;

        ttsExecutor.submit(() -> {
            try {
                byte[] wav = requestSpeechWav(text);
                if (wav == null || wav.length == 0) {
                    fallbackNarratorOrSystemSpeech(text);
                    return;
                }
                playCrushedWav(wav);
            } catch (Exception ignored) {
                fallbackNarratorOrSystemSpeech(text);
            }
        });
    }

    private byte[] requestSpeechWav(String text) throws Exception {
        String apiKey = resolveApiKey();
        if (apiKey.isBlank()) {
            return null;
        }
        byte[] primary = requestSpeechWav(text, apiKey, DEFAULT_TTS_MODEL);
        if (primary != null && primary.length > 0) {
            return primary;
        }
        return requestSpeechWav(text, apiKey, FALLBACK_TTS_MODEL);
    }

    private byte[] requestSpeechWav(String text, String apiKey, String model) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        payload.addProperty("voice", "alloy");
        payload.addProperty("input", text);
        payload.addProperty("response_format", "wav");
        payload.addProperty("instructions",
                "Speak like a glitched retro terminal entity. Crunchy, bit-crushed, unstable. "
                        + "Randomly swing pitch too low or too high. Keep it eerie and short.");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEFAULT_TTS_ENDPOINT))
                .timeout(Duration.ofSeconds(16))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 300) {
            return null;
        }
        return response.body();
    }

    private void playCrushedWav(byte[] wavBytes) throws Exception {
        try (AudioInputStream originalStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBytes))) {
            AudioFormat baseFormat = originalStream.getFormat();
            AudioFormat pcm16 = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcm16, originalStream)) {
                byte[] rawPcm = pcmStream.readAllBytes();
                applyBitCrush(rawPcm, pcm16.getChannels());

                float pitchFactor = random.nextBoolean()
                        ? randomBetween(0.68f, 0.84f)
                        : randomBetween(1.18f, 1.42f);
                AudioFormat playbackFormat = new AudioFormat(
                        pcm16.getEncoding(),
                        pcm16.getSampleRate() * pitchFactor,
                        pcm16.getSampleSizeInBits(),
                        pcm16.getChannels(),
                        pcm16.getFrameSize(),
                        pcm16.getFrameRate() * pitchFactor,
                        pcm16.isBigEndian()
                );

                Clip clip = AudioSystem.getClip();
                clip.open(playbackFormat, rawPcm, 0, rawPcm.length);
                clip.start();
                clip.drain();
                clip.close();
            }
        }
    }

    private void applyBitCrush(byte[] pcm, int channels) {
        int bitDepth = random.nextBoolean() ? 6 : 7;
        int crushMask = ~((1 << (16 - bitDepth)) - 1);
        int holdFrames = random.nextInt(3, 7);
        int bytesPerFrame = channels * 2;

        int heldSample = 0;
        int holdCounter = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int frameOffset = (i / bytesPerFrame) % holdFrames;
            if (frameOffset == 0 || holdCounter <= 0) {
                int sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
                sample &= crushMask;
                heldSample = sample;
                holdCounter = holdFrames;
            } else {
                holdCounter--;
            }

            pcm[i] = (byte) (heldSample & 0xFF);
            pcm[i + 1] = (byte) ((heldSample >> 8) & 0xFF);
        }
    }

    private String resolveApiKey() {
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }

        try {
            Minecraft minecraft = Minecraft.getInstance();
            File gameDir = minecraft.gameDirectory;
            File file = new File(new File(gameDir, "config"), CONFIG_FILE);
            if (!file.exists()) {
                return "";
            }
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonObject openai = root.has("openai") ? root.getAsJsonObject("openai") : new JsonObject();
            return openai.has("apiKey") ? openai.get("apiKey").getAsString().trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        String cleaned = input.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (cleaned.length() > MAX_TEXT_LEN) {
            return cleaned.substring(0, MAX_TEXT_LEN);
        }
        return cleaned;
    }

    private void fallbackNarratorOrSystemSpeech(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getNarrator().sayNow(Component.literal(text));
        }
        fallbackWindowsSpeech(text);
    }

    private void fallbackWindowsSpeech(String text) {
        try {
            String escaped = text.replace("'", "''");
            String command = "$spk = New-Object -ComObject SAPI.SpVoice; $spk.Rate = -1; $spk.Speak('"
                    + escaped + "') | Out-Null";
            new ProcessBuilder("powershell", "-NoProfile", "-Command", command).start();
        } catch (Exception ignored) {
            // final fallback remains narrator only
        }
    }

    private float randomBetween(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
