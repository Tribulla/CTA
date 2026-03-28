package com.cta.client.renderer;

import com.cta.CTA;
import com.cta.client.CameraHandler;
import com.cta.client.ScopeViewManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CTA.MODID)
public class ScopeOverlayRenderer {

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_ACCENT = 0xFF8BE9FD;

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!ScopeViewManager.isViewingScope()) {
            return;
        }

        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
            event.setCanceled(true);
            return;
        }

        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        renderReticle(guiGraphics, width, height);
        renderRangeMarkers(guiGraphics, width, height);
        renderZoom(guiGraphics, width);
    }

    private static void renderReticle(GuiGraphics guiGraphics, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);

        int gap = 7;
        int horizontalReach = 46;
        int upperReach = 38;
        int lowerReach = 78;

        guiGraphics.fill(centerX - horizontalReach, centerY, centerX - gap, centerY + 1, COLOR_ACCENT);
        guiGraphics.fill(centerX + gap, centerY, centerX + horizontalReach, centerY + 1, COLOR_ACCENT);
        guiGraphics.fill(centerX, centerY - upperReach, centerX + 1, centerY - gap, COLOR_ACCENT);
        guiGraphics.fill(centerX, centerY + gap, centerX + 1, centerY + lowerReach, COLOR_ACCENT);
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, COLOR_WHITE);

        for (int i = 1; i <= 4; i++) {
            int offset = i * 18;
            int tickHalfWidth = i % 2 == 0 ? 10 : 6;
            guiGraphics.fill(centerX - tickHalfWidth, centerY + offset, centerX + tickHalfWidth + 1, centerY + offset + 1, COLOR_ACCENT);
            guiGraphics.fill(centerX - tickHalfWidth / 2, centerY - offset, centerX + tickHalfWidth / 2 + 1, centerY - offset + 1, 0xAA7AD7E8);
        }

        int bracketOffset = 54;
        int bracketHeight = 16;
        guiGraphics.fill(centerX - bracketOffset, centerY - bracketHeight, centerX - bracketOffset + 1, centerY + bracketHeight, COLOR_ACCENT);
        guiGraphics.fill(centerX + bracketOffset, centerY - bracketHeight, centerX + bracketOffset + 1, centerY + bracketHeight, COLOR_ACCENT);
        guiGraphics.fill(centerX - bracketOffset, centerY - bracketHeight, centerX - bracketOffset + 9, centerY - bracketHeight + 1, COLOR_ACCENT);
        guiGraphics.fill(centerX - bracketOffset, centerY + bracketHeight, centerX - bracketOffset + 9, centerY + bracketHeight + 1, COLOR_ACCENT);
        guiGraphics.fill(centerX + bracketOffset - 8, centerY - bracketHeight, centerX + bracketOffset + 1, centerY - bracketHeight + 1, COLOR_ACCENT);
        guiGraphics.fill(centerX + bracketOffset - 8, centerY + bracketHeight, centerX + bracketOffset + 1, centerY + bracketHeight + 1, COLOR_ACCENT);

        int ringRadius = 32;
        for (int angle = 0; angle < 360; angle += 12) {
            double radians = Math.toRadians(angle);
            int x = centerX + (int) Math.round(Math.cos(radians) * ringRadius);
            int y = centerY + (int) Math.round(Math.sin(radians) * ringRadius);
            guiGraphics.fill(x, y, x + 1, y + 1, 0x8896E6F5);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderZoom(GuiGraphics guiGraphics, int width) {
        Minecraft mc = Minecraft.getInstance();
        float zoom = CameraHandler.getCurrentZoom();
        String zoomText = String.format("%.1fx", zoom);
        int x = (width - mc.font.width(zoomText)) / 2;
        guiGraphics.drawString(mc.font, zoomText, x, 12, COLOR_WHITE, false);
    }

    private static void renderRangeMarkers(GuiGraphics guiGraphics, int width, int height) {
        int[] levels = ScopeViewManager.getRangeLevels();
        double[] rangeValues = ScopeViewManager.getRangeValues();
        int loadedCharges = ScopeViewManager.getLoadedCharges();

        if (!ScopeViewManager.hasBoundCannon() || levels.length == 0) return;

        Minecraft mc = Minecraft.getInstance();
        int centerX = width / 2;
        int centerY = height / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Label each downward tick (offsets 18, 36, 54, 72) with the per-charge range
        for (int i = 0; i < Math.min(4, levels.length); i++) {
            int offset = (i + 1) * 18;
            int tickY = centerY + offset;
            boolean isLoaded = levels[i] == loadedCharges;
            int color = isLoaded ? 0xFFFFD700 : COLOR_ACCENT;

            if (rangeValues[i] > 0) {
                String label = String.format("%dm", (int) Math.round(rangeValues[i]));
                guiGraphics.drawString(mc.font, label, centerX + 16, tickY - 4, color, false);
            }
        }

        // Current range readout to the right of the horizontal arm
        double currentRange = ScopeViewManager.getCurrentRange();
        if (currentRange > 0) {
            String rangeStr = String.format("~%dm", (int) Math.round(currentRange));
            guiGraphics.drawString(mc.font, rangeStr, centerX + 50, centerY - 4, 0xFFFFD700, true);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
