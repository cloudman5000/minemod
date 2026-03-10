package com.example.examplemod.client.screen;

import com.example.examplemod.client.audio.RetroTtsService;
import com.example.examplemod.terminal.block.HackingTerminalBlockEntity;
import com.example.examplemod.terminal.domain.AiAvailabilityState;
import com.example.examplemod.terminal.domain.TerminalActivity;
import com.example.examplemod.terminal.menu.HackingTerminalMenu;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retro-styled terminal screen with a simple REPL.
 */
public class HackingTerminalScreen extends AbstractContainerScreen<HackingTerminalMenu> {
    private static final int SCREEN_WIDTH = 320;
    private static final int SCREEN_HEIGHT = 180;
    private static final int PADDING = 10;
    private static final int BG_COLOR = 0xE6000000;
    private static final int FRAME_COLOR = 0xFF1A3B1A;
    private static final int AI_TEXT_COLOR = 0xFF4CCB4C;
    private static final int USER_TEXT_COLOR = 0xFFA8FFA8;
    private static final int SYSTEM_TEXT_COLOR = 0xFF6BFF6B;
    private static final int GAME_TEXT_COLOR = 0xFF72B6FF;
    private static final int CORRUPTION_TEXT_COLOR = 0xFFFFB46E;
    private static final int ERROR_TEXT_COLOR = 0xFFFF5C5C;
    private static final int CURSOR_COLOR = 0xFF8EFF8E;
    private static final int INPUT_BG_COLOR = 0xAA112211;
    private static final int ARCADE_BG = 0xFF071607;
    private static final int ARCADE_FRAME = 0xFF2D572D;
    private static final int ARCADE_ACCENT = 0xFF9CFF9C;
    private static final int ARCADE_INVADER = 0xFF8ED1FF;

    private final StringBuilder inputBuffer = new StringBuilder();
    private int cursorBlinkTick = 0;
    private List<String> historySnapshot = new ArrayList<>();
    private int scrollOffsetLines = 0;
    private int lastKnownHistorySize = 0;
    private boolean greeted = false;
    private String runtimeStatus = "STANDBY";
    private TerminalActivity terminalActivity = TerminalActivity.IDLE;
    private String activeMinigameId = "";
    private String activeMinigameKind = "";
    private String activeMinigameStateJson = "";

    public HackingTerminalScreen(HackingTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
        this.inventoryLabelY = this.imageHeight + 1000;
        this.titleLabelY = this.imageHeight + 1000;
    }

    @Override
    protected void init() {
        super.init();
        setFocused(null);
        speakRetro("terminal online. your quirky assistant is ready.");
        greeted = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleArcadeKeyPress(keyCode)) {
            return true;
        }
        if (keyCode == 256) { // Escape
            onClose();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // Enter
            String submitted = inputBuffer.toString();
            if (!submitted.trim().isEmpty()) {
                submitToServer(submitted);
            }
            inputBuffer.setLength(0);
            if ("exit".equalsIgnoreCase(submitted.trim())) {
                onClose();
            }
            return true;
        }
        if (keyCode == 266) { // Page Up
            scrollOffsetLines += 3;
            return true;
        }
        if (keyCode == 267) { // Page Down
            scrollOffsetLines = Math.max(0, scrollOffsetLines - 3);
            return true;
        }
        if (keyCode == 259) { // Backspace
            if (!inputBuffer.isEmpty()) {
                inputBuffer.setLength(inputBuffer.length() - 1);
            }
            return true;
        }
        // Keep keyboard focus exclusive to the terminal while it is open.
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint >= 32 && codePoint != 127 && inputBuffer.length() < 120) {
            inputBuffer.append(codePoint);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            scrollOffsetLines += 2;
        } else if (scrollY < 0) {
            scrollOffsetLines = Math.max(0, scrollOffsetLines - 2);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isArcadeActive() && (keyCode == 263 || keyCode == 65 || keyCode == 262 || keyCode == 68)) {
            sendArcadeControl("__arcade move 0");
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        int right = left + this.imageWidth;
        int bottom = top + this.imageHeight;

        guiGraphics.fill(left - 2, top - 2, right + 2, bottom + 2, FRAME_COLOR);
        guiGraphics.fill(left, top, right, bottom, BG_COLOR);

        int outputTop = top + PADDING;
        if (isArcadeActive()) {
            int arcadeTop = outputTop + 10;
            int arcadeHeight = 78;
            drawArcadeViewport(guiGraphics, left + PADDING, arcadeTop, this.imageWidth - (PADDING * 2), arcadeHeight);
            outputTop = arcadeTop + arcadeHeight + 8;
        }
        int outputBottom = bottom - PADDING - 28;
        int inputTop = outputBottom + 4;
        guiGraphics.fill(left + PADDING - 3, inputTop - 2, right - PADDING + 3, bottom - PADDING + 2, INPUT_BG_COLOR);

        String loadingFrame = switch ((cursorBlinkTick / 6) % 4) {
            case 0 -> ".";
            case 1 -> "..";
            case 2 -> "...";
            default -> "....";
        };
        String header = "ASSIST:" + runtimeStatus;
        if (terminalActivity == TerminalActivity.RUNNING_WORKFLOW) {
            header += "  processing" + loadingFrame;
        } else if (isArcadeActive()) {
            header += "  arcade:" + activeMinigameId;
        }
        guiGraphics.drawString(this.font, header, left + PADDING, top + 1, SYSTEM_TEXT_COLOR, false);
        drawOutputLines(guiGraphics, left + PADDING, outputTop, outputBottom);
        drawInputLine(guiGraphics, left + PADDING, inputTop);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        cursorBlinkTick++;
        refreshHistoryFromBlockEntity();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (isArcadeActive()) {
            submitToServer("quit");
        }
        super.onClose();
    }

    private void drawOutputLines(GuiGraphics graphics, int x, int top, int bottom) {
        int lineHeight = this.font.lineHeight + 1;
        int maxLines = Math.max(1, (bottom - top) / lineHeight);
        int maxWidth = this.imageWidth - (PADDING * 2) - 8;
        List<DisplayLine> wrapped = buildWrappedHistory(maxWidth);
        int maxScroll = Math.max(0, wrapped.size() - maxLines);
        scrollOffsetLines = Mth.clamp(scrollOffsetLines, 0, maxScroll);
        int endExclusive = wrapped.size() - scrollOffsetLines;
        int startIndex = Math.max(0, endExclusive - maxLines);

        graphics.enableScissor(
                this.leftPos + PADDING,
                top,
                this.leftPos + this.imageWidth - PADDING,
                bottom
        );
        int currentY = top;
        for (int i = startIndex; i < endExclusive; i++) {
            DisplayLine line = wrapped.get(i);
            graphics.drawString(this.font, line.text(), x, currentY, line.color(), false);
            currentY += lineHeight;
        }
        graphics.disableScissor();
    }

    private void drawInputLine(GuiGraphics graphics, int x, int y) {
        int inputWidth = this.imageWidth - (PADDING * 2) - 10;
        String inputText = trimFromStartToFit("> " + inputBuffer, inputWidth);
        graphics.drawString(this.font, inputText, x, y, USER_TEXT_COLOR, false);

        if ((cursorBlinkTick / 6) % 2 == 0) {
            int cursorX = x + this.font.width(inputText) + 1;
            graphics.fill(cursorX, y, cursorX + 6, y + this.font.lineHeight, CURSOR_COLOR);
        }
    }

    private void refreshHistoryFromBlockEntity() {
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        BlockEntity blockEntity = minecraft.level.getBlockEntity(menu.terminalPos());
        if (blockEntity instanceof HackingTerminalBlockEntity terminalBlockEntity) {
            int previousSize = lastKnownHistorySize;
            historySnapshot = terminalBlockEntity.historySnapshot();
            lastKnownHistorySize = historySnapshot.size();
            runtimeStatus = displayRuntimeStatus(terminalBlockEntity.aiAvailabilityState());
            terminalActivity = terminalBlockEntity.activity();
            activeMinigameId = terminalBlockEntity.activeMinigameId();
            activeMinigameKind = terminalBlockEntity.activeMinigameKind();
            activeMinigameStateJson = terminalBlockEntity.minigameStateJson();
            if (!greeted) {
                speakRetro("terminal online. your quirky assistant is ready.");
                greeted = true;
            }
            maybeSpeakNewTtsLines(previousSize);
        }
    }

    private String displayRuntimeStatus(AiAvailabilityState state) {
        if (state == null) {
            return "STANDBY";
        }
        return switch (state) {
            case ONLINE -> "ONLINE";
            case DEGRADED -> "UNSTABLE";
            case DISABLED -> "STANDBY";
        };
    }

    private void submitToServer(String command) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        String sanitized = command
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
        if (sanitized.isEmpty()) {
            return;
        }
        String serverCommand = "examplemod_terminal submit "
                + menu.terminalPos().getX() + " "
                + menu.terminalPos().getY() + " "
                + menu.terminalPos().getZ() + " "
                + sanitized;
        minecraft.player.connection.sendCommand(serverCommand);
    }

    private boolean handleArcadeKeyPress(int keyCode) {
        if (!isArcadeActive()) {
            return false;
        }
        if (keyCode == 263 || keyCode == 65) { // left / A
            sendArcadeControl("__arcade move -1");
            return true;
        }
        if (keyCode == 262 || keyCode == 68) { // right / D
            sendArcadeControl("__arcade move 1");
            return true;
        }
        if (keyCode == 32 || keyCode == 265 || keyCode == 87) { // space/up/W
            sendArcadeControl("__arcade fire");
            return true;
        }
        return false;
    }

    private void sendArcadeControl(String command) {
        submitToServer(command);
    }

    private boolean isArcadeActive() {
        return activeMinigameId != null && !activeMinigameId.isBlank();
    }

    private void drawArcadeViewport(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, ARCADE_FRAME);
        graphics.fill(x, y, x + width, y + height, ARCADE_BG);
        if ("pong".equalsIgnoreCase(activeMinigameKind)) {
            drawPongFrame(graphics, x, y, width, height);
        } else if ("invaders".equalsIgnoreCase(activeMinigameKind)) {
            drawInvadersFrame(graphics, x, y, width, height);
        }
    }

    private void drawPongFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        JsonObject state = parseState(activeMinigameStateJson);
        double ballX = jsonDouble(state, "ballX", 0.5);
        double ballY = jsonDouble(state, "ballY", 0.5);
        double paddle = jsonDouble(state, "paddle", 0.5);
        int score = jsonInt(state, "score", 0);
        int lives = jsonInt(state, "lives", 0);

        int bx = x + (int) (ballX * width);
        int by = y + (int) (ballY * height);
        int paddleCenter = x + (int) (paddle * width);
        int paddleWidth = Math.max(18, width / 6);
        int paddleY = y + height - 10;

        graphics.fill(paddleCenter - (paddleWidth / 2), paddleY, paddleCenter + (paddleWidth / 2), paddleY + 4, ARCADE_ACCENT);
        graphics.fill(bx - 2, by - 2, bx + 2, by + 2, ARCADE_ACCENT);
        graphics.drawString(this.font, "PONG  score:" + score + "  lives:" + lives, x + 4, y + 3, ARCADE_ACCENT, false);
    }

    private void drawInvadersFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        JsonObject state = parseState(activeMinigameStateJson);
        int shipX = jsonInt(state, "shipX", 6);
        int lives = jsonInt(state, "lives", 0);
        int score = jsonInt(state, "score", 0);
        int gridW = 13;
        int gridH = 11;
        int cellW = Math.max(4, width / gridW);
        int cellH = Math.max(4, height / gridH);

        JsonArray invaders = state != null && state.has("invaders") && state.get("invaders").isJsonArray()
                ? state.getAsJsonArray("invaders") : new JsonArray();
        for (int i = 0; i < invaders.size(); i++) {
            JsonObject inv = invaders.get(i).isJsonObject() ? invaders.get(i).getAsJsonObject() : null;
            if (inv == null) {
                continue;
            }
            int ix = jsonInt(inv, "x", 0);
            int iy = jsonInt(inv, "y", 0);
            int px = x + (ix * cellW) + 1;
            int py = y + (iy * cellH) + 1;
            graphics.fill(px, py, px + cellW - 2, py + cellH - 2, ARCADE_INVADER);
        }

        JsonArray shots = state != null && state.has("shots") && state.get("shots").isJsonArray()
                ? state.getAsJsonArray("shots") : new JsonArray();
        for (int i = 0; i < shots.size(); i++) {
            JsonObject shot = shots.get(i).isJsonObject() ? shots.get(i).getAsJsonObject() : null;
            if (shot == null) {
                continue;
            }
            int sx = jsonInt(shot, "x", 0);
            int sy = jsonInt(shot, "y", 0);
            int px = x + (sx * cellW) + (cellW / 2);
            int py = y + (sy * cellH);
            graphics.fill(px, py, px + 2, py + Math.max(4, cellH - 1), ARCADE_ACCENT);
        }

        int shipPx = x + (shipX * cellW) + 1;
        int shipPy = y + ((gridH - 1) * cellH);
        graphics.fill(shipPx, shipPy, shipPx + cellW - 2, shipPy + cellH - 2, ARCADE_ACCENT);
        graphics.drawString(this.font, "INVADERS  score:" + score + "  lives:" + lives, x + 4, y + 3, ARCADE_ACCENT, false);
    }

    private JsonObject parseState(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private int jsonInt(JsonObject obj, String key, int fallback) {
        try {
            if (obj != null && obj.has(key)) {
                return obj.get(key).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private double jsonDouble(JsonObject obj, String key, double fallback) {
        try {
            if (obj != null && obj.has(key)) {
                return obj.get(key).getAsDouble();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private List<DisplayLine> buildWrappedHistory(int maxWidth) {
        List<DisplayLine> lines = new ArrayList<>();
        List<String> collapsed = collapseLiveLines(historySnapshot);
        for (String raw : collapsed) {
            List<String> wrapped = hardWrap(raw, maxWidth);
            if (wrapped.isEmpty()) {
                lines.add(new DisplayLine("", AI_TEXT_COLOR));
                continue;
            }
            int color = colorForLine(raw);
            for (String segment : wrapped) {
                lines.add(new DisplayLine(stripControlPrefix(segment), color));
            }
        }
        return lines;
    }

    private List<String> collapseLiveLines(List<String> sourceLines) {
        List<String> collapsed = new ArrayList<>();
        Map<String, Integer> liveIndexById = new HashMap<>();
        for (String raw : sourceLines) {
            LiveUpdate update = parseLiveUpdate(raw);
            if (update == null) {
                collapsed.add(raw);
                continue;
            }
            Integer existing = liveIndexById.get(update.id());
            if (update.text().isBlank()) {
                if (existing != null) {
                    collapsed.set(existing, "");
                }
                continue;
            }
            if (existing == null) {
                liveIndexById.put(update.id(), collapsed.size());
                collapsed.add(update.text());
            } else {
                collapsed.set(existing, update.text());
            }
        }
        return collapsed.stream().filter(line -> line != null && !line.isBlank()).toList();
    }

    private List<String> hardWrap(String raw, int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        String source = raw == null ? "" : raw;
        if (source.isEmpty()) {
            wrapped.add("");
            return wrapped;
        }

        String remaining = source;
        while (!remaining.isEmpty()) {
            int cut = remaining.length();
            while (cut > 1 && this.font.width(remaining.substring(0, cut)) > maxWidth) {
                cut--;
            }
            wrapped.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut);
        }
        return wrapped;
    }

    private static String trimFromStartToFit(String text, int maxWidthPx, net.minecraft.client.gui.Font font) {
        String out = text;
        while (!out.isEmpty() && font.width(out) > maxWidthPx) {
            out = out.substring(1);
        }
        return out;
    }

    private String trimFromStartToFit(String text, int maxWidthPx) {
        return trimFromStartToFit(text, maxWidthPx, this.font);
    }

    private void maybeSpeakNewTtsLines(int previousSize) {
        if (historySnapshot.isEmpty() || previousSize >= historySnapshot.size()) {
            return;
        }
        for (int i = Math.max(0, previousSize); i < historySnapshot.size(); i++) {
            String line = historySnapshot.get(i);
            if (line.toLowerCase().startsWith("[voice]")) {
                speakRetro(line.substring(7).trim());
            }
        }
    }

    private void speakRetro(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        RetroTtsService.get().speak(text.replaceAll("[^a-zA-Z0-9 .,!?\\-]", " "));
    }

    private int colorForLine(String line) {
        String normalized = line == null ? "" : line.toLowerCase();
        if (normalized.startsWith("[error]")) {
            return ERROR_TEXT_COLOR;
        }
        if (normalized.startsWith("[game]")) {
            return GAME_TEXT_COLOR;
        }
        if (normalized.startsWith("[reward]") || normalized.startsWith("[corruption]")) {
            return CORRUPTION_TEXT_COLOR;
        }
        if (normalized.startsWith("[system]") || normalized.startsWith("[flavor]")) {
            return SYSTEM_TEXT_COLOR;
        }
        if (normalized.startsWith("root@")) {
            return USER_TEXT_COLOR;
        }
        if (normalized.startsWith("cmd>")) {
            return SYSTEM_TEXT_COLOR;
        }
        return AI_TEXT_COLOR;
    }

    private String stripControlPrefix(String line) {
        if (line == null) {
            return "";
        }
        if (line.toLowerCase().startsWith("[error]")) {
            return line.substring(7).trim();
        }
        if (line.toLowerCase().startsWith("[system]")) {
            return line.substring(8).trim();
        }
        if (line.toLowerCase().startsWith("[game]")) {
            return line.substring(6).trim();
        }
        if (line.toLowerCase().startsWith("[reward]")) {
            return line.substring(8).trim();
        }
        if (line.toLowerCase().startsWith("[corruption]")) {
            return line.substring(12).trim();
        }
        if (line.toLowerCase().startsWith("[flavor]")) {
            return line.substring(8).trim();
        }
        if (line.toLowerCase().startsWith("[voice]")) {
            return line.substring(7).trim();
        }
        return line;
    }

    private LiveUpdate parseLiveUpdate(String line) {
        if (line == null) {
            return null;
        }
        String normalized = line.trim();
        if (!normalized.toLowerCase().startsWith("[live:")) {
            return null;
        }
        int pipe = normalized.indexOf('|');
        if (pipe < 0) {
            return null;
        }
        String id = normalized.substring(6, pipe).trim();
        if (id.isEmpty()) {
            return null;
        }
        String text = normalized.substring(pipe + 1).trim();
        if (text.endsWith("]")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return new LiveUpdate(id, text);
    }

    private record DisplayLine(String text, int color) {
    }

    private record LiveUpdate(String id, String text) {
    }
}
