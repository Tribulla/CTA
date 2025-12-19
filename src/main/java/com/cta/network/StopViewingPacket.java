package com.cta.network;

import com.cta.entity.CameraEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopViewingPacket {
    public StopViewingPacket() {
    }

    public static void encode(StopViewingPacket msg, FriendlyByteBuf buf) {
    }

    public static StopViewingPacket decode(FriendlyByteBuf buf) {
        return new StopViewingPacket();
    }

    public static void handle(StopViewingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity camera = player.getCamera();
                if (camera instanceof CameraEntity) {
                    ((CameraEntity) camera).stopViewing(player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
