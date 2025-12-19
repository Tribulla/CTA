package com.cta.client;

import com.cta.CTA;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CTA.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyInit {
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CameraHandler.KEY_SCOPE_UP);
        event.register(CameraHandler.KEY_SCOPE_DOWN);
        event.register(CameraHandler.KEY_EXIT_SCOPE);
    }
}
