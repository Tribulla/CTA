package com.cta;

import com.cta.registry.ModEntities;
import com.cta.registry.ModItems;
import com.cta.registry.ModBlocks;
import com.cta.registry.ModBlockEntities;
import com.cta.registry.ModCreativeTabs;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.cta.client.renderer.MissileRenderer;
import com.cta.client.renderer.ScopeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import com.cta.network.PacketHandler;

@Mod(CTA.MODID)
public class CTA {
    public static final String MODID = "cta";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CTA() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModCreativeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
        // Common setup code
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Server starting code
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Client setup code
            EntityRenderers.register(ModEntities.MISSILE.get(), MissileRenderer::new);
            EntityRenderers.register(ModEntities.SCOPE.get(), ScopeRenderer::new);
        }
    }
}
