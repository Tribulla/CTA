package com.cta.client.renderer;

import com.cta.CTA;
import com.cta.client.CameraHandler;
import com.cta.client.ScopeViewManager;
import com.cta.compat.CBCCompat;
import com.cta.block.ScopeBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CTA.MODID)
public class ScopeOverlayRenderer {
    
    private static final ResourceLocation SCOPE_OVERLAY = new ResourceLocation(CTA.MODID, "textures/gui/scope_overlay.png");
    
    private static final ResourceLocation RETICLE = new ResourceLocation(CTA.MODID, "textures/gui/scope_reticle.png");
    
    private static final int COLOR_GREEN = 0xFF00FF00;
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    private static final int COLOR_RED = 0xFFFF0000;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
            return;
        }
        
        if (!ScopeViewManager.isViewingScope()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        
        renderScopeVignette(guiGraphics, width, height);
        
        renderReticle(guiGraphics, width, height);
        
        renderScopeInfo(guiGraphics, width, height);
        
        ScopeBlockEntity scopeForRange = ScopeViewManager.getViewedScopeBE();
        if (scopeForRange != null) {
            BlockPos cannonMount = scopeForRange.getBoundCannonMount();
            if (cannonMount != null && CBCCompat.isCBCLoaded()) {
                renderRangeIndicator(guiGraphics, mc.level, cannonMount, width, height);
            }
        }
    }
    
    private static void renderScopeVignette(GuiGraphics guiGraphics, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 2 - 10;
        
        guiGraphics.fill(0, 0, centerX - radius, height, 0xFF000000);
        guiGraphics.fill(centerX + radius, 0, width, height, 0xFF000000);
        guiGraphics.fill(centerX - radius, 0, centerX + radius, centerY - radius, 0xFF000000);
        guiGraphics.fill(centerX - radius, centerY + radius, centerX + radius, height, 0xFF000000);
        
        RenderSystem.disableBlend();
    }
    
    private static void renderReticle(GuiGraphics guiGraphics, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);
        
        int lineLength = 20;
        int gap = 4;
        int color = 0xFFFFFFFF;
        
        guiGraphics.fill(centerX - lineLength - gap, centerY, centerX - gap, centerY + 1, color);
        guiGraphics.fill(centerX + gap, centerY, centerX + lineLength + gap, centerY + 1, color);
        
        guiGraphics.fill(centerX, centerY - lineLength - gap, centerX + 1, centerY - gap, color);
        guiGraphics.fill(centerX, centerY + gap, centerX + 1, centerY + lineLength + gap, color);
        
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
        
        for (int i = 1; i <= 3; i++) {
            int offset = i * 15;

            guiGraphics.fill(centerX - offset - 1, centerY + 2, centerX - offset + 2, centerY + 5, color);
            guiGraphics.fill(centerX + offset - 1, centerY + 2, centerX + offset + 2, centerY + 5, color);
            guiGraphics.fill(centerX + 2, centerY + offset - 1, centerX + 5, centerY + offset + 2, color);
        }
        
        int circleRadius = 30;
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI * i) / segments;
            double angle2 = (2 * Math.PI * (i + 1)) / segments;
            int x1 = centerX + (int)(circleRadius * Math.cos(angle1));
            int y1 = centerY + (int)(circleRadius * Math.sin(angle1));
            int x2 = centerX + (int)(circleRadius * Math.cos(angle2));
            int y2 = centerY + (int)(circleRadius * Math.sin(angle2));
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    
    private static void renderScopeInfo(GuiGraphics guiGraphics, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        ScopeBlockEntity scopeBE = ScopeViewManager.getViewedScopeBE();
        if (scopeBE == null) return;
        
        int margin = 10;
        int textY = margin;
        
        float zoom = CameraHandler.getCurrentZoom();
        String zoomText = String.format("%.1fx", zoom);
        guiGraphics.drawString(mc.font, zoomText, margin, textY, COLOR_WHITE, true);
        textY += 12;
        
        float yaw = scopeBE.getScopeYaw();
        float pitch = scopeBE.getScopePitch();
        String angleText = String.format("YAW: %.1f° PITCH: %.1f°", yaw, pitch);
        guiGraphics.drawString(mc.font, angleText, margin, textY, COLOR_GRAY, true);
        textY += 12;
        
        BlockPos cannonMount = scopeBE.getBoundCannonMount();
        if (cannonMount != null) {
            String boundText = "CANNON BOUND";
            guiGraphics.drawString(mc.font, boundText, margin, textY, COLOR_GREEN, true);
        }
        
        String exitHint = "Press SHIFT to exit";
        int exitWidth = mc.font.width(exitHint);
        guiGraphics.drawString(mc.font, exitHint, (width - exitWidth) / 2, height - 20, COLOR_GRAY, true);
    }
    
    private static void renderRangeIndicator(GuiGraphics guiGraphics, Level level, BlockPos cannonMount, int width, int height) {
        if (level == null || !CBCCompat.isCannonMount(level, cannonMount)) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        
        Float cannonYaw = CBCCompat.getCannonMountYaw(level, cannonMount);
        Float cannonPitch = CBCCompat.getCannonMountPitch(level, cannonMount);
        
        if (cannonYaw == null || cannonPitch == null) {
            return;
        }
        
        int rightMargin = width - 10;
        int textY = 10;
        
        guiGraphics.drawString(mc.font, "RANGE (est.)", rightMargin - 80, textY, COLOR_WHITE, true);
        textY += 14;
        
        for (int charges = 1; charges <= 5; charges++) {
            double range = CBCCompat.calculateSimpleRange(level, cannonMount, charges);
            
            if (range >= 0) {
                String rangeText;
                int color;
                
                if (range < 50) {
                    rangeText = String.format("%dC: %.0fm", charges, range);
                    color = COLOR_GREEN;
                } else if (range < 150) {
                    rangeText = String.format("%dC: %.0fm", charges, range);
                    color = COLOR_YELLOW;
                } else {
                    rangeText = String.format("%dC: %.0fm", charges, range);
                    color = COLOR_RED;
                }
                
                int textWidth = mc.font.width(rangeText);
                guiGraphics.drawString(mc.font, rangeText, rightMargin - textWidth, textY, color, true);
                textY += 10;
            }
        }
        
        textY += 5;
        String pitchText = String.format("ELEV: %.1f°", -cannonPitch);
        int pitchWidth = mc.font.width(pitchText);
        guiGraphics.drawString(mc.font, pitchText, rightMargin - pitchWidth, textY, COLOR_WHITE, true);
    }
}
