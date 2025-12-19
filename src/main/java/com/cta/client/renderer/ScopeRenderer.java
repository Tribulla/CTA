package com.cta.client.renderer;

import com.cta.entity.ScopeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ScopeRenderer extends EntityRenderer<ScopeEntity> {
    private final ItemRenderer itemRenderer;

    public ScopeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ScopeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack modelItem = entity.getModelItem();
        if (modelItem.isEmpty()) {
            // Use default scope item if no model item set
            modelItem = new ItemStack(com.cta.registry.ModItems.PANTHER_SCOPE.get());
        }
        
        poseStack.pushPose();
        
        // Get rotation from entity
        float yaw = entity.getStoredYaw();
        float pitch = entity.getStoredPitch();
        
        // Apply rotation to align model with entity facing direction
        // Model nose points -Z (North), need to rotate to face entity's yaw direction
        // Formula: 180 - yaw to face the correct direction
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        
        // Scale appropriately
        poseStack.scale(1.0f, 1.0f, 1.0f);
        
        this.itemRenderer.renderStatic(
            modelItem,
            ItemDisplayContext.FIXED,
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

    @Override
    public ResourceLocation getTextureLocation(ScopeEntity entity) {
        return new ResourceLocation("cta", "textures/item/panther_scope.png");
    }
}
