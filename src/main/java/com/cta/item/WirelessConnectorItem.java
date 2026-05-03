package com.cta.item;

import com.cta.block.ScopeBlockEntity;
import com.cta.compat.CBCCompat;
import com.cta.network.OpenChannelScreenPacket;
import com.cta.network.PacketHandler;
import com.cta.wireless.WirelessLinkSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class WirelessConnectorItem extends Item {

    public WirelessConnectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ── Clear Tool Memory (Shift-Right-Click in Air) ─────────────────────────
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && (tag.contains("ScopeBindPos") || tag.contains("PeripheralPos"))) {
                tag.remove("ScopeBindPos");
                tag.remove("ScopeBindDim");
                tag.remove("PeripheralPos");
                tag.remove("PeripheralDim");
                player.displayClientMessage(Component.literal("Cleared selected connections from tool.").withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        String currentDim = level.dimension().location().toString();

        // ── INSPECT MODE (Right-Click without Sneaking) ──────────────────────
        if (!player.isShiftKeyDown()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            
            BlockEntity clickedBe = level.getBlockEntity(clickedPos);
            
            // Inspect Scope
            if (clickedBe instanceof ScopeBlockEntity scopeBE) {
                BlockPos bound = scopeBE.getBoundCannonMount();
                if (bound != null) {
                    player.displayClientMessage(Component.literal("Scope is bound to Cannon at " + bound.toShortString()).withStyle(ChatFormatting.AQUA), true);
                } else {
                    player.displayClientMessage(Component.literal("Scope is currently unbound.").withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.SUCCESS;
            }

            // Inspect CC Peripheral
            if (level instanceof ServerLevel serverLevel) {
                WirelessLinkSavedData data = WirelessLinkSavedData.get(serverLevel);
                if (data.isPeripheral(clickedPos)) {
                    player.displayClientMessage(Component.literal("This block is connected to the CC wireless network.").withStyle(ChatFormatting.GREEN), true);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }

        // ── DRIVE BY WIRE BINDING MODE (Shift-Right-Click Block) ─────────────
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity clickedBe = level.getBlockEntity(clickedPos);
        boolean isScope = clickedBe instanceof ScopeBlockEntity;
        boolean isCannon = CBCCompat.isCBCLoaded() && CBCCompat.isCannonMount(level, clickedPos);

        // 1. SCOPE -> CANNON LOGIC
        if (isScope) {
            tag.putLong("ScopeBindPos", clickedPos.asLong());
            tag.putString("ScopeBindDim", currentDim);
            tag.remove("PeripheralPos"); // Clear CC selection so modes don't mix
            tag.remove("PeripheralDim");
            player.displayClientMessage(Component.literal("Scope selected at " + clickedPos.toShortString()).withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.SUCCESS;
        }

        if (isCannon && tag.contains("ScopeBindPos")) {
            if (!currentDim.equals(tag.getString("ScopeBindDim"))) {
                player.displayClientMessage(Component.literal("Scope and cannon must be in the same dimension!").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            BlockPos scopePos = BlockPos.of(tag.getLong("ScopeBindPos"));
            if (!(level.getBlockEntity(scopePos) instanceof ScopeBlockEntity scopeBE)) {
                tag.remove("ScopeBindPos");
                player.displayClientMessage(Component.literal("Selected scope no longer exists.").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            scopeBE.bindToCannonMount(clickedPos);
            tag.remove("ScopeBindPos"); // Auto-clear after successful link!
            tag.remove("ScopeBindDim");
            player.displayClientMessage(Component.literal("Scope bound to cannon at " + clickedPos.toShortString()).withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }


        // 2. CC PERIPHERAL LOGIC
        // If they double-click the exact same block, sever its connections
        if (tag.contains("PeripheralPos") && BlockPos.of(tag.getLong("PeripheralPos")).equals(clickedPos)) {
            if (level instanceof ServerLevel serverLevel) {
                WirelessLinkSavedData data = WirelessLinkSavedData.get(serverLevel);
                int removed = data.removeAllLinksForPeripheral(clickedPos);
                tag.remove("PeripheralPos");
                tag.remove("PeripheralDim");
                player.displayClientMessage(Component.literal("Removed " + removed + " links from this peripheral.").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!tag.contains("PeripheralPos")) {
            // Step 1: Select Source
            tag.putLong("PeripheralPos", clickedPos.asLong());
            tag.putString("PeripheralDim", currentDim);
            tag.remove("ScopeBindPos"); // Clear Scope selection so modes don't mix
            tag.remove("ScopeBindDim");

            String blockName = ForgeRegistries.BLOCKS.getKey(level.getBlockState(clickedPos).getBlock()).getPath();
            player.displayClientMessage(Component.literal("Selected " + blockName + " as Source. Click target to link...").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        } else {
            // Step 2: Select Target & Form Link
            if (!currentDim.equals(tag.getString("PeripheralDim"))) {
                player.displayClientMessage(Component.literal("Both blocks must be in the same dimension!").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockPos peripheralPos = BlockPos.of(tag.getLong("PeripheralPos"));
            String blockName = ForgeRegistries.BLOCKS.getKey(level.getBlockState(clickedPos).getBlock()).getPath();

            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new OpenChannelScreenPacket(peripheralPos, clickedPos));
                
                player.displayClientMessage(Component.literal("Linking Source to " + blockName + "...").withStyle(ChatFormatting.GREEN), true);
            }

            // Auto-clear tool memory so they can immediately wire up the next node pair!
            tag.remove("PeripheralPos");
            tag.remove("PeripheralDim");

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("ScopeBindPos")) {
            BlockPos pos = BlockPos.of(tag.getLong("ScopeBindPos"));
            tooltip.add(Component.literal("Scope Source: " + pos.toShortString()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Shift+click a Cannon Mount to finish bind").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("Shift+click air to cancel").withStyle(ChatFormatting.DARK_GRAY));
        } else if (tag != null && tag.contains("PeripheralPos")) {
            BlockPos pos = BlockPos.of(tag.getLong("PeripheralPos"));
            tooltip.add(Component.literal("CC Source: " + pos.toShortString()).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("Shift+click target to connect").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("Shift+click source again to disconnect").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Shift+click air to cancel").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Shift+click blocks to create wireless links").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Right-click a linked block to inspect it").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}