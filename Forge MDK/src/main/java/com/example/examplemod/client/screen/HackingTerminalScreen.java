package com.example.examplemod.client.screen;

import com.example.examplemod.terminal.menu.HackingTerminalMenu;
import com.example.examplemod.terminal.repl.EchoTerminalCommandProcessor;
import com.example.examplemod.terminal.repl.TerminalSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Retro-styled terminal screen with a simple REPL.
 */
public class HackingTerminalScreen extends AbstractContainerScreen<HackingTerminalMenu> {
    private static final int SCREEN_WIDTH = 320;
    private static final int SCREEN_HEIGHT = 180;
    private static final int PADDING = 10;
    private static final int BG_COLOR = 0xE6000000;
    private static final int FRAME_COLOR = 0xFF1A3B1A;
    private static final int TEXT_COLOR = 0xFF6BFF6B;
    private static final int CURSOR_COLOR = 0xFF8EFF8E;
    private static final int INPUT_BG_COLOR = 0xAA112211;

    private final TerminalSession terminalSession = new TerminalSession(new EchoTerminalCommandProcessor(), "root@reality> ");
    private final StringBuilder inputBuffer = new StringBuilder();
    private int cursorBlinkTick = 0;

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
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            onClose();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // Enter
            boolean shouldClose = terminalSession.submit(inputBuffer.toString());
            inputBuffer.setLength(0);
            if (shouldClose) {
                onClose();
            }
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
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        int right = left + this.imageWidth;
        int bottom = top + this.imageHeight;

        guiGraphics.fill(left - 2, top - 2, right + 2, bottom + 2, FRAME_COLOR);
        guiGraphics.fill(left, top, right, bottom, BG_COLOR);

        int outputTop = top + PADDING;
        int outputBottom = bottom - PADDING - 16;
        int inputTop = outputBottom + 4;
        guiGraphics.fill(left + PADDING - 3, inputTop - 2, right - PADDING + 3, bottom - PADDING + 2, INPUT_BG_COLOR);

        drawOutputLines(guiGraphics, left + PADDING, outputTop, outputBottom);
        drawInputLine(guiGraphics, left + PADDING, inputTop);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        cursorBlinkTick++;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawOutputLines(GuiGraphics graphics, int x, int top, int bottom) {
        int lineHeight = this.font.lineHeight + 1;
        int maxLines = Math.max(1, (bottom - top) / lineHeight);

        List<String> history = terminalSession.getHistory();
        int startIndex = Math.max(0, history.size() - maxLines);

        int currentY = top;
        for (int i = startIndex; i < history.size(); i++) {
            graphics.drawString(this.font, history.get(i), x, currentY, TEXT_COLOR, false);
            currentY += lineHeight;
        }
    }

    private void drawInputLine(GuiGraphics graphics, int x, int y) {
        String inputText = "> " + inputBuffer;
        graphics.drawString(this.font, inputText, x, y, TEXT_COLOR, false);

        if ((cursorBlinkTick / 6) % 2 == 0) {
            int cursorX = x + this.font.width(inputText) + 1;
            graphics.fill(cursorX, y, cursorX + 6, y + this.font.lineHeight, CURSOR_COLOR);
        }
    }
}
