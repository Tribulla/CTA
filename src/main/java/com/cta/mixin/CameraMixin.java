package com.cta.mixin;

import com.cta.client.ScopeViewManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Camera.class)
public abstract class CameraMixin {
    
    @Shadow
    private Vec3 position;
    
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);
    
    @Shadow
    protected abstract void setPosition(double x, double y, double z);
    
    @Inject(method = "setup", at = @At("TAIL"))
    private void cta$overrideCameraForScope(BlockGetter level, Entity entity, boolean detached, 
                                             boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (ScopeViewManager.isViewingScope()) {
            Vec3 scopePos = ScopeViewManager.getCameraPosition();
            float yaw = ScopeViewManager.getCameraYaw();
            float pitch = ScopeViewManager.getCameraPitch();
            
            setPosition(scopePos.x, scopePos.y, scopePos.z);
            
            setRotation(yaw, pitch);
        }
    }
}
