package com.cta.network;

import com.cta.block.ScopeBlockEntity;
import com.cta.entity.CameraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class StopViewingPacket {
    @Nullable
    private final BlockPos scopePos;

    public StopViewingPacket() {
        this.scopePos = null;
    }

    public StopViewingPacket(@Nullable BlockPos scopePos) {
        this.scopePos = scopePos;
    }

    public static void encode(StopViewingPacket msg, FriendlyByteBuf buf) {
        boolean hasPos = msg.scopePos != null;
        buf.writeBoolean(hasPos);
        if (hasPos) {
            buf.writeBlockPos(msg.scopePos);
        }
    }

    public static StopViewingPacket decode(FriendlyByteBuf buf) {
        boolean hasPos = buf.readBoolean();
        if (hasPos) {
            return new StopViewingPacket(buf.readBlockPos());
        }
        return new StopViewingPacket();
    }

    public static void handle(StopViewingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // If we have a specific scope position, target it directly
                if (msg.scopePos != null) {
                    BlockEntity be = player.level().getBlockEntity(msg.scopePos);
                    if (be instanceof ScopeBlockEntity scopeBE) {
                        scopeBE.stopViewing(player);
                    }
                } else {
                    // Fallback: scan nearby scope blocks
                    BlockPos center = player.blockPosition();
                    for (int dx = -8; dx <= 8; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            for (int dz = -8; dz <= 8; dz++) {
                                BlockPos checkPos = center.offset(dx, dy, dz);
                                BlockEntity be = player.level().getBlockEntity(checkPos);
                                if (be instanceof ScopeBlockEntity scopeBE) {
                                    scopeBE.stopViewing(player);
                                }
                            }
                        }
                    }
                }

                // LEGACY: Handle old CameraEntity system
                Entity camera = player.getCamera();
                if (camera instanceof CameraEntity) {
                    ((CameraEntity) camera).stopViewing(player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
