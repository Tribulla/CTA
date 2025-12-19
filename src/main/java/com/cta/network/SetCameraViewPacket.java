package com.cta.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetCameraViewPacket {
    private final int entityId;

    public SetCameraViewPacket(Entity entity) {
        this.entityId = entity != null ? entity.getId() : -1;
    }

    public SetCameraViewPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SetCameraViewPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static SetCameraViewPacket decode(FriendlyByteBuf buf) {
        return new SetCameraViewPacket(buf.readInt());
    }

    public static void handle(SetCameraViewPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = null;
            if (msg.entityId != -1) {
                entity = Minecraft.getInstance().level.getEntity(msg.entityId);
            }
            Minecraft.getInstance().setCameraEntity(entity != null ? entity : Minecraft.getInstance().player);
        });
        ctx.get().setPacketHandled(true);
    }
}
