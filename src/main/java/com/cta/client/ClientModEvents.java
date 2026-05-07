package com.cta.client;

import com.cta.CTA;
import com.cta.registry.ModEntities;
import com.cta.client.renderer.MissileRenderer;
import com.cta.client.renderer.ScopeRenderer;
import com.cta.client.renderer.FragmentRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CTA.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.MISSILE.get(), MissileRenderer::new);
        EntityRenderers.register(ModEntities.SCOPE.get(), ScopeRenderer::new);
        EntityRenderers.register(ModEntities.THERMAL_SCOPE.get(), ScopeRenderer::new);
        EntityRenderers.register(ModEntities.FRAGMENT.get(), FragmentRenderer::new);
    }
}
