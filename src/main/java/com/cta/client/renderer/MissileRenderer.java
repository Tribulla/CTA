package com.cta.client.renderer;

import com.cta.entity.MissileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

public class MissileRenderer extends EntityRenderer<MissileEntity> {
    private final ItemRenderer itemRenderer;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack modelItem = entity.getModelItem();
        if (modelItem.isEmpty()) {
            return;
        }
        
        poseStack.pushPose();
        
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float roll = 0.0f; // needs to be unbfucked later if I remember about it
        
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        
        float scale = 1.0f;
        if (entity.isDeployed()) {
            scale = getVelocityBasedScale(entity.getDeltaMovement());
        }
        poseStack.scale(scale, scale, scale);
        
        this.itemRenderer.renderStatic(
            modelItem, 
            ItemDisplayContext.GROUND, 
            packedLight, 
            OverlayTexture.NO_OVERLAY, 
            poseStack, 
            buffer, 
            entity.level(), 
            entity.getId()
        );
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    public float getVelocityBasedScale(net.minecraft.world.phys.Vec3 velocity) {
        double speed = velocity.length();
        return (float) Math.min(1.0 + speed * 0.1, 1.5);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        // Not used
        return new ResourceLocation("cta", "textures/item/missile.png");
    }
}
