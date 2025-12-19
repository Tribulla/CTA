package com.cta.client;

import com.cta.entity.CameraEntity;
import com.cta.network.PacketHandler;
import com.cta.network.StopViewingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * CameraHandler - Based on Tallyho's CameraClientHandler
 * Handles client-side camera control when viewing through a CameraEntity
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CameraHandler {
    public static final KeyMapping KEY_SCOPE_UP = new KeyMapping("key.cta.scope_up", GLFW.GLFW_KEY_UP, "key.categories.cta");
    public static final KeyMapping KEY_SCOPE_DOWN = new KeyMapping("key.cta.scope_down", GLFW.GLFW_KEY_DOWN, "key.categories.cta");
    public static final KeyMapping KEY_EXIT_SCOPE = new KeyMapping("key.cta.exit_scope", GLFW.GLFW_KEY_BACKSPACE, "key.categories.cta");

    private static float currentZoom = 1.0f;
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 10.0f;
    private static final float ZOOM_STEP = 0.5f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity cameraEntity) {
            // Exit on Shift, Escape, or custom key
            if (mc.options.keyShift.isDown() || KEY_EXIT_SCOPE.isDown()) {
                PacketHandler.INSTANCE.sendToServer(new StopViewingPacket());
                currentZoom = 1.0f;
                return;
            }
            
            // Zoom controls
            if (KEY_SCOPE_UP.consumeClick()) {
                currentZoom = Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM);
            }
            if (KEY_SCOPE_DOWN.consumeClick()) {
                currentZoom = Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM);
            }
        } else {
            // Reset zoom when not in scope
            if (currentZoom != 1.0f) {
                currentZoom = 1.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        
        if (camera instanceof CameraEntity cameraEntity) {
            // Apply custom FOV from camera entity if set
            int customFov = cameraEntity.getFOV();
            if (customFov > 0 && customFov < 70) {
                event.setFOV(customFov);
            }
            
            // Apply zoom
            if (currentZoom > 1.0f) {
                event.setFOV(event.getFOV() / currentZoom);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        
        if (camera instanceof CameraEntity) {
            double scroll = event.getScrollDelta();
            if (scroll > 0) {
                currentZoom = Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM);
            } else if (scroll < 0) {
                currentZoom = Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM);
            }
            event.setCanceled(true);
        }
    }

    /**
     * Get current zoom level
     */
    public static float getCurrentZoom() {
        return currentZoom;
    }

    /**
     * Reset zoom to default
     */
    public static void resetZoom() {
        currentZoom = 1.0f;
    }
}
