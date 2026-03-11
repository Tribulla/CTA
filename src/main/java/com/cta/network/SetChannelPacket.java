package com.cta.network;

import com.cta.item.WirelessConnectorItem;
import com.cta.wireless.WirelessLinkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → Server packet: registers a wireless link with a channel name.
 */
public class SetChannelPacket {
    private final BlockPos peripheralPos;
    private final BlockPos targetPos;
    private final String channel;

    public SetChannelPacket(BlockPos peripheralPos, BlockPos targetPos, String channel) {
        this.peripheralPos = peripheralPos;
        this.targetPos = targetPos;
        this.channel = channel;
    }

    public static void encode(SetChannelPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.peripheralPos);
        buf.writeBlockPos(msg.targetPos);
        buf.writeUtf(msg.channel, 32);
    }

    public static SetChannelPacket decode(FriendlyByteBuf buf) {
        return new SetChannelPacket(buf.readBlockPos(), buf.readBlockPos(), buf.readUtf(32));
    }

    public static void handle(SetChannelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Validate proximity to target
            if (player.distanceToSqr(msg.targetPos.getX() + 0.5, msg.targetPos.getY() + 0.5, msg.targetPos.getZ() + 0.5) > 64) return;

            String sanitized = msg.channel.replaceAll("[^a-zA-Z0-9_\\-]", "");
            if (sanitized.isEmpty() || sanitized.length() > 32) return;

            // Register the wireless link
            ServerLevel level = player.serverLevel();
            WirelessLinkSavedData data = WirelessLinkSavedData.get(level);
            data.addLink(msg.targetPos, sanitized, msg.peripheralPos);

            // Clear item NBT
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof WirelessConnectorItem) {
                CompoundTag tag = held.getTag();
                if (tag != null) {
                    tag.remove("PeripheralPos");
                    tag.remove("PeripheralDim");
                }
            } else {
                // Check offhand
                held = player.getOffhandItem();
                if (held.getItem() instanceof WirelessConnectorItem) {
                    CompoundTag tag = held.getTag();
                    if (tag != null) {
                        tag.remove("PeripheralPos");
                        tag.remove("PeripheralDim");
                    }
                }
            }

            player.displayClientMessage(Component.literal("Channel '" + sanitized + "' connected!"), true);

            // Trigger block update near target to cause CC peripheral re-scan
            level.updateNeighborsAt(msg.targetPos, level.getBlockState(msg.targetPos).getBlock());
        });
        ctx.get().setPacketHandled(true);
    }
}
