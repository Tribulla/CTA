package com.cta.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetCameraViewPacket {
    public final int entityId;

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
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.cta.client.ClientPacketHandler.handleSetCameraViewPacket(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
