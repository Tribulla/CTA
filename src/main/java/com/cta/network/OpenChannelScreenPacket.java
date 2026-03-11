package com.cta.network;

import com.cta.client.screen.SetChannelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client packet: tells the client to open the channel name GUI.
 */
public class OpenChannelScreenPacket {
    private final BlockPos peripheralPos;
    private final BlockPos targetPos;

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
            SetChannelScreen.open(msg.peripheralPos, msg.targetPos);
        });
        ctx.get().setPacketHandled(true);
    }
}
