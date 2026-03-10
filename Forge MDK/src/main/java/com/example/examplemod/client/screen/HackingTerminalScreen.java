package com.example.examplemod.client.screen;

import com.example.examplemod.client.audio.RetroTtsService;
import com.example.examplemod.terminal.block.HackingTerminalBlockEntity;
import com.example.examplemod.terminal.menu.HackingTerminalMenu;
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
    private static final int ERROR_TEXT_COLOR = 0xFFFF5C5C;
    private static final int CURSOR_COLOR = 0xFF8EFF8E;
    private static final int INPUT_BG_COLOR = 0xAA112211;

    private final StringBuilder inputBuffer = new StringBuilder();
    private int cursorBlinkTick = 0;
    private List<String> historySnapshot = new ArrayList<>();
    private int scrollOffsetLines = 0;
    private int lastKnownHistorySize = 0;
    private boolean greeted = false;
    private String runtimeStatus = "DISABLED";

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
        speakRetro("terminal online. reality restoration interface ready.");
        greeted = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        int right = left + this.imageWidth;
        int bottom = top + this.imageHeight;

        guiGraphics.fill(left - 2, top - 2, right + 2, bottom + 2, FRAME_COLOR);
        guiGraphics.fill(left, top, right, bottom, BG_COLOR);

        int outputTop = top + PADDING;
        int outputBottom = bottom - PADDING - 28;
        int inputTop = outputBottom + 4;
        guiGraphics.fill(left + PADDING - 3, inputTop - 2, right - PADDING + 3, bottom - PADDING + 2, INPUT_BG_COLOR);

        guiGraphics.drawString(this.font, "AI: " + runtimeStatus, left + PADDING, top + 1, SYSTEM_TEXT_COLOR, false);
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
            runtimeStatus = terminalBlockEntity.aiAvailabilityState().name();
            if (!greeted) {
                speakRetro("terminal online. reality restoration interface ready.");
                greeted = true;
            }
            maybeSpeakNewTtsLines(previousSize);
        }
    }

    private void submitToServer(String command) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        String sanitized = command.replace("\"", "'");
        String serverCommand = "examplemod_terminal submit "
                + menu.terminalPos().getX() + " "
                + menu.terminalPos().getY() + " "
                + menu.terminalPos().getZ() + " "
                + "\"" + sanitized + "\"";
        minecraft.player.connection.sendCommand(serverCommand);
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
