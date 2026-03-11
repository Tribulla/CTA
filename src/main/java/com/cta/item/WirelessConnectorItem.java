package com.cta.item;

import com.cta.network.OpenChannelScreenPacket;
import com.cta.network.PacketHandler;
import com.cta.wireless.WirelessLinkSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class WirelessConnectorItem extends Item {

    public WirelessConnectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        CompoundTag tag = stack.getOrCreateTag();

        if (level instanceof ServerLevel serverLevel) {
            WirelessLinkSavedData data = WirelessLinkSavedData.get(serverLevel);
            if (data.isPeripheral(clickedPos)) {
                int removed = data.removeAllLinksForPeripheral(clickedPos);
                String blockName = ForgeRegistries.BLOCKS.getKey(level.getBlockState(clickedPos).getBlock()).toString();
                player.displayClientMessage(
                        Component.literal("Removed " + blockName + " at " + clickedPos.toShortString() + " (" + removed + " link" + (removed != 1 ? "s" : "") + ")")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }
        }

        if (!tag.contains("PeripheralPos")) {
            tag.putLong("PeripheralPos", clickedPos.asLong());
            tag.putString("PeripheralDim", level.dimension().location().toString());

            String blockName = ForgeRegistries.BLOCKS.getKey(level.getBlockState(clickedPos).getBlock()).toString();
            player.displayClientMessage(
                    Component.literal("Selected " + blockName + " at " + clickedPos.toShortString()).withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        } else {
            String storedDim = tag.getString("PeripheralDim");
            if (!level.dimension().location().toString().equals(storedDim)) {
                player.displayClientMessage(
                        Component.literal("Both blocks must be in the same dimension!").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockPos peripheralPos = BlockPos.of(tag.getLong("PeripheralPos"));

            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new OpenChannelScreenPacket(peripheralPos, clickedPos));
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("PeripheralPos")) {
            BlockPos pos = BlockPos.of(tag.getLong("PeripheralPos"));
            tooltip.add(Component.literal("Linked to: " + pos.toShortString()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Shift+click target to connect").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal("Shift+click a block to start linking").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Shift+click linked peripheral to remove").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
