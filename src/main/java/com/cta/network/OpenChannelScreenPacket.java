package com.cta.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenChannelScreenPacket {
    public final BlockPos peripheralPos;
    public final BlockPos targetPos;

    public OpenChannelScreenPacket(BlockPos peripheralPos, BlockPos targetPos) {
        this.peripheralPos = peripheralPos;
        this.targetPos = targetPos;
    }

    public static void encode(OpenChannelScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.peripheralPos);
        buf.writeBlockPos(msg.targetPos);
    }

    public static OpenChannelScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenChannelScreenPacket(buf.readBlockPos(), buf.readBlockPos());
    }

    public static void handle(OpenChannelScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.cta.client.ClientPacketHandler.handleOpenChannelScreenPacket(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
