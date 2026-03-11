package com.cta.network;

import com.cta.client.ScopeViewManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to start viewing through a scope block.
 * Sends the scope's BlockPos.
 */
public class StartScopeViewPacket {
    private final BlockPos scopePos;

    public StartScopeViewPacket(BlockPos scopePos) {
        this.scopePos = scopePos;
    }

    public static void encode(StartScopeViewPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.scopePos);
    }

    public static StartScopeViewPacket decode(FriendlyByteBuf buf) {
        return new StartScopeViewPacket(buf.readBlockPos());
    }

    public static void handle(StartScopeViewPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(StartScopeViewPacket msg) {
        ScopeViewManager.startViewing(msg.scopePos);
    }
}
