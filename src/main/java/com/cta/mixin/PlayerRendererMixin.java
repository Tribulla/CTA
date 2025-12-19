package com.cta.mixin;

import com.cta.entity.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Mixin to hide the player model when viewing through a CameraEntity (scope)
 * This prevents the player's body from blocking the view
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void cta$hidePlayerWhenInScope(AbstractClientPlayer player, float entityYaw, float partialTicks, 
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        
        // If we're viewing through a camera entity
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity instanceof CameraEntity) {
            // Hide the local player (the one viewing through the scope)
            if (player == mc.player) {
                ci.cancel();
            }
        }
    }
}
