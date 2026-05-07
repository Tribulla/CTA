package com.cta;

import com.cta.config.MissileConfig;
import com.cta.registry.ModBlockEntities;
import com.cta.registry.ModBlocks;
import com.cta.registry.ModEntities;
import com.cta.registry.ModItems;
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
import com.cta.network.PacketHandler;
import net.minecraftforge.fml.ModList;

@Mod(CTA.MODID)
public class CTA {
    public static final String MODID = "cta";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CTA() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MissileConfig.register();

        ModCreativeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PacketHandler.register();
            if (ModList.get().isLoaded("computercraft")) {
                com.cta.compat.cc.CCCompat.init();
            }
        });
        LOGGER.info("CTA Missile Config loaded successfully");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
