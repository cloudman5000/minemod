package com.example.examplemod.client.screen;

import com.example.examplemod.config.ExampleModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HorrorConfigScreen extends Screen {

    private final Screen lastScreen;

    public HorrorConfigScreen(Screen lastScreen) {
        super(Component.literal("Horror Mod Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();
        int startY = this.height / 6;

        // Jumpscare Toggle Button
        boolean isJumpscareEnabled = ExampleModConfig.ENABLE_JUMPSCARES.get();
        this.addRenderableWidget(Button.builder(
                Component.literal("Loud Jumpscares: " + (isJumpscareEnabled ? "ON" : "OFF")),
                button -> {
                    boolean newState = !ExampleModConfig.ENABLE_JUMPSCARES.get();
                    ExampleModConfig.ENABLE_JUMPSCARES.set(newState);
                    button.setMessage(Component.literal("Loud Jumpscares: " + (newState ? "ON" : "OFF")));
                }).bounds(this.width / 2 - 100, startY, 200, 20)
                .tooltip(Tooltip.create(
                        Component.literal("Toggle terrifying audio and visual jumpscares (Warning: very loud).")))
                .build());

        // Fake Permission Screen Toggle Button
        boolean isFakePermsEnabled = ExampleModConfig.ALLOW_FAKE_PERMISSIONS.get();
        this.addRenderableWidget(Button.builder(
                Component.literal("Fake UI Scares: " + (isFakePermsEnabled ? "ON" : "OFF")),
                button -> {
                    boolean newState = !ExampleModConfig.ALLOW_FAKE_PERMISSIONS.get();
                    ExampleModConfig.ALLOW_FAKE_PERMISSIONS.set(newState);
                    button.setMessage(Component.literal("Fake UI Scares: " + (newState ? "ON" : "OFF")));
                }).bounds(this.width / 2 - 100, startY + 24, 200, 20)
                .tooltip(Tooltip.create(
                        Component.literal("Toggle fake OS permission request dialog boxes that simulate malware.")))
                .build());

        // Glitch Spawn Rate Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Glitch Attack Frequency: " + getAttackFrequencyLabel()),
                button -> {
                    cycleGlitchAttackFrequency();
                    button.setMessage(Component.literal("Glitch Attack Frequency: " + getAttackFrequencyLabel()));
                }).bounds(this.width / 2 - 100, startY + 48, 200, 20)
                .tooltip(Tooltip.create(
                        Component.literal("Adjusts how often the Glitch decides to naturally attack the player.")))
                .build());

        // Tile Glitching Button
        boolean isTileGlitchEnabled = ExampleModConfig.ENABLE_TILE_GLITCHES.get();
        this.addRenderableWidget(Button.builder(
                Component.literal("Environment Glitching: " + (isTileGlitchEnabled ? "ON" : "OFF")),
                button -> {
                    boolean newState = !ExampleModConfig.ENABLE_TILE_GLITCHES.get();
                    ExampleModConfig.ENABLE_TILE_GLITCHES.set(newState);
                    button.setMessage(Component.literal("Environment Glitching: " + (newState ? "ON" : "OFF")));
                }).bounds(this.width / 2 - 100, startY + 72, 200, 20)
                .tooltip(Tooltip.create(
                        Component.literal("Allow blocks around the Glitch to become visually corrupted.")))
                .build());

        // Signer Spawn Rate Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Signer Ambush Chance: " + getSignerSpawnChanceLabel()),
                button -> {
                    cycleSignerSpawnChance();
                    button.setMessage(Component.literal("Signer Ambush Chance: " + getSignerSpawnChanceLabel()));
                }).bounds(this.width / 2 - 100, startY + 96, 200, 20)
                .tooltip(Tooltip.create(
                        Component.literal("Adjusts how often Clue Signs appear behind you when paranoid.")))
                .build());

        // Back / Save Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Save & Exit"),
                button -> {
                    ExampleModConfig.SPEC.save();
                    this.minecraft.setScreen(this.lastScreen);
                }).bounds(this.width / 2 - 100, startY + 144, 200, 20).build());
    }

    private String getSignerSpawnChanceLabel() {
        int rate = ExampleModConfig.CLUE_SIGN_CHANCE.get();
        if (rate <= 50)
            return "Extreme";
        if (rate <= 150)
            return "High";
        if (rate <= 500)
            return "Normal";
        return "Low";
    }

    private void cycleSignerSpawnChance() {
        int rate = ExampleModConfig.CLUE_SIGN_CHANCE.get();
        if (rate <= 50) {
            ExampleModConfig.CLUE_SIGN_CHANCE.set(150); // Jump to High
        } else if (rate <= 150) {
            ExampleModConfig.CLUE_SIGN_CHANCE.set(500); // Jump to Normal
        } else if (rate <= 500) {
            ExampleModConfig.CLUE_SIGN_CHANCE.set(1500); // Jump to Low
        } else {
            ExampleModConfig.CLUE_SIGN_CHANCE.set(50); // Jump back to Extreme
        }
    }

    private String getAttackFrequencyLabel() {
        int rate = ExampleModConfig.GLITCH_SPAWN_RATE.get();
        if (rate <= 20)
            return "Low";
        if (rate <= 60)
            return "Normal";
        return "High";
    }

    private void cycleGlitchAttackFrequency() {
        int rate = ExampleModConfig.GLITCH_SPAWN_RATE.get();
        if (rate <= 20) {
            ExampleModConfig.GLITCH_SPAWN_RATE.set(50); // Jump to Normal
        } else if (rate <= 60) {
            ExampleModConfig.GLITCH_SPAWN_RATE.set(90); // Jump to High
        } else {
            ExampleModConfig.GLITCH_SPAWN_RATE.set(10); // Loop back to Low
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ExampleModConfig.SPEC.save();
        Minecraft.getInstance().setScreen(this.lastScreen);
    }
}
