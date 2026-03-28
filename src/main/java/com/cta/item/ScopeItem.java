package com.cta.item;

import com.cta.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ScopeItem extends BlockItem {

    public ScopeItem(Properties properties) {
        super(ModBlocks.SCOPE_BLOCK.get(), properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cta.scope.place").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cta.scope.use").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Use the Wireless Connector to bind to a cannon mount").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cta.scope.pickup").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
