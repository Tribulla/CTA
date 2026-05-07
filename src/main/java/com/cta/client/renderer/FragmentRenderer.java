package com.cta.client.renderer;

import com.cta.entity.FragmentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FragmentRenderer extends EntityRenderer<FragmentEntity> {
    
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/particle/generic_0.png");
    
    public FragmentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void render(FragmentEntity entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }
    
    @Override
    public ResourceLocation getTextureLocation(FragmentEntity entity) {
        return TEXTURE;
    }
}
