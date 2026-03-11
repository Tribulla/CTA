package com.cta.mixin;

import com.cta.client.ScopeViewManager;
import com.cta.entity.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cta$blockAttackInScope(CallbackInfoReturnable<Boolean> cir) {
        if (ScopeViewManager.isViewingScope()) {
            cir.setReturnValue(false);
            return;
        }
        
        Minecraft mc = (Minecraft)(Object)this;
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            cir.setReturnValue(false);
        }
    }
    
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cta$blockUseInScope(CallbackInfo ci) {
        if (ScopeViewManager.isViewingScope()) {
            ci.cancel();
            return;
        }
        
        Minecraft mc = (Minecraft)(Object)this;
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            ci.cancel();
        }
    }
    
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cta$blockContinueAttackInScope(boolean leftClick, CallbackInfo ci) {
        if (ScopeViewManager.isViewingScope()) {
            ci.cancel();
            return;
        }
        
        Minecraft mc = (Minecraft)(Object)this;
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            ci.cancel();
        }
    }
    
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void cta$blockPickInScope(CallbackInfo ci) {
        if (ScopeViewManager.isViewingScope()) {
            ci.cancel();
            return;
        }
        
        Minecraft mc = (Minecraft)(Object)this;
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            ci.cancel();
        }
    }
    
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cta$blockScreenInScope(@Nullable Screen screen, CallbackInfo ci) {

        if (screen == null) return;
        
        if (ScopeViewManager.isViewingScope()) {
            ci.cancel();
            return;
        }
        
        Minecraft mc = (Minecraft)(Object)this;
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            ci.cancel();
        }
    }
    
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void cta$blockKeybindsInScope(CallbackInfo ci) {
        if (ScopeViewManager.isViewingScope()) {
            ci.cancel();
            return;
        }
        
        Minecraft mc = (Minecraft)(Object)this;
        Entity camera = mc.getCameraEntity();
        if (camera instanceof CameraEntity && camera != mc.player) {
            ci.cancel();
        }
    }
}
