package com.cta.mixin;

import com.cta.client.ScopeViewManager;
import com.cta.entity.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void cta$preventMovementWhenInCamera(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        
        if (ScopeViewManager.isViewingScope()) {
            self.input.leftImpulse = 0;
            self.input.forwardImpulse = 0;
            self.input.jumping = false;
            self.input.shiftKeyDown = false;
            self.setSprinting(false);
            return;
        }
        
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            self.input.leftImpulse = 0;
            self.input.forwardImpulse = 0;
            self.input.jumping = false;
            self.input.shiftKeyDown = false;
            self.setSprinting(false);
        }
    }
}
