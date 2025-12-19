// player is still deleported for some reason but fixed in another file

package com.cta.mixin;

import com.cta.entity.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent player movement when viewing through a CameraEntity (scope)
 * This stops the player from "teleporting" into the scope when using movement keys
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    
    /**
     * Prevent movement input processing when viewing through a camera
     * We zero out movement inputs so the player doesn't drift while looking through scope
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void cta$preventMovementWhenInCamera(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        
        // If viewing through our camera entity, prevent movement
        if (camera instanceof CameraEntity && camera != mc.player) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            
            // Reset movement inputs so player doesn't move
            // Note: We keep shiftKeyDown so the CameraHandler can detect it for exiting
            self.input.leftImpulse = 0;
            self.input.forwardImpulse = 0;
            self.input.jumping = false;
            // Don't reset shiftKeyDown here - it's needed for exit detection in CameraHandler
        }
    }
}
