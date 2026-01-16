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

/**
 * MissileRenderer - Based on Tallyho's MissileEntityRenderer
 * Renders the missile's model item with proper rotation.
 * 
 * Coordinate system notes:
 * - Minecraft yaw: 0 = South (+Z), 90 = West (-X), 180 = North (-Z), 270 = East (+X)
 * - Minecraft pitch: positive = looking down, negative = looking up
 * - Our models point along -Z axis (North) by default in Blockbench
 * - We use GROUND display context to avoid JSON display.fixed.rotation overrides
 */
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
        
        // Interpolate rotation for smooth rendering
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float roll = 0.0f; // Roll disabled per user request
        
        // Rotation to align model with entity facing direction:
        // Model points -Z (North = yaw 180). To point in direction yaw, rotate by (180 - yaw).
        // The entity yaw convention: 0=South, 90=West, 180=North, 270=East
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        // Apply pitch rotation around X axis
        // Negate pitch because Minecraft's pitch convention (positive = looking down) is opposite
        // to the rotation direction needed after the yaw transform
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        // Apply roll (bank) rotation around Z axis (forward axis)
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        
        // Scale based on velocity when deployed (slight stretch at high speed)
        float scale = 1.0f;
        if (entity.isDeployed()) {
            scale = getVelocityBasedScale(entity.getDeltaMovement());
        }
        poseStack.scale(scale, scale, scale);
        
        // Use GROUND context to bypass display.fixed.rotation in JSON models
        // This gives us full control over orientation
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

    /**
     * Scale factor based on velocity - gives slight stretch at high speeds
     */
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
