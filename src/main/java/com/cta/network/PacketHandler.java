package com.cta.network;

import com.cta.CTA;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CTA.MODID + ":main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, SetCameraViewPacket.class, SetCameraViewPacket::encode, SetCameraViewPacket::decode, SetCameraViewPacket::handle);
        INSTANCE.registerMessage(id++, StopViewingPacket.class, StopViewingPacket::encode, StopViewingPacket::decode, StopViewingPacket::handle);
    }
}
