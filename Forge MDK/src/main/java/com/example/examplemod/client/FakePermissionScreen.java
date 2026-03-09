package com.example.examplemod.client;

import com.example.examplemod.entity.GlitchEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FakePermissionScreen extends Screen {

    private boolean isClosing = false;

    public FakePermissionScreen() {
        super(Component.literal("System Permission Required"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Position buttons like a standard dialog
        this.addRenderableWidget(Button.builder(Component.literal("Allow"), button -> triggerScareAndClose())
                .bounds(centerX - 110, centerY + 30, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Deny"), button -> triggerScareAndClose())
                .bounds(centerX + 10, centerY + 30, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft == null)
            return;

        // Darken the background to mimic a modal dialog overlay
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 260;
        int boxHeight = 100;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // Draw a fake "Windows/OS" style dialog box
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFE0E0E0); // Light gray background
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + 20, 0xFF0055AA); // Blue title bar
        guiGraphics.renderOutline(boxX, boxY, boxWidth, boxHeight, 0xFF000000); // Black border

        // Title text
        guiGraphics.drawString(this.font, "User Account Control", boxX + 5, boxY + 6, 0xFFFFFF, false);

        // Warning text
        Component warningText1 = Component.literal("Do you want to allow Glitch to access your files?");
        Component warningText2 = Component.literal("Modifying system properties may cause irreversible damage.");

        guiGraphics.drawWordWrap(this.font, warningText1, boxX + 15, boxY + 35, boxWidth - 30, 0x000000);
        guiGraphics.drawWordWrap(this.font, warningText2, boxX + 15, boxY + 55, boxWidth - 30, 0x555555);

        // Render buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        triggerScareAndClose();
        return false; // Custom close logic handles it
    }

    private void triggerScareAndClose() {
        if (isClosing || this.minecraft == null || this.minecraft.player == null)
            return;
        isClosing = true;

        // Play a very loud, sudden noise when they click
        this.minecraft.level.playLocalSound(
                this.minecraft.player.getX(),
                this.minecraft.player.getY(),
                this.minecraft.player.getZ(),
                SoundEvents.ENDERMAN_STARE,
                SoundSource.HOSTILE,
                5.0F, 0.5F, false);

        // Find nearby glitch entity to aggro it if it was stalking
        AABB searchBox = this.minecraft.player.getBoundingBox().inflate(30);
        List<GlitchEntity> nearby = this.minecraft.level.getEntitiesOfClass(GlitchEntity.class, searchBox,
                GlitchEntity::isAlive);
        if (!nearby.isEmpty()) {
            GlitchEntity glitch = nearby.get(0);
            glitch.setManifesting(true); // Force manifestation
        }

        this.onClose();
    }
}
