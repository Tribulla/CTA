package com.cta.mixin;

import com.cta.entity.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to lock mouse movement when viewing through a scope
 * The view should be fixed, not free-look
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void cta$lockViewInScope(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            // Cancel mouse turning completely - view is locked
            ci.cancel();
        }
    }
}
