package com.cta.item;

import com.cta.entity.ScopeEntity;
import com.cta.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ScopeItem - Places a ScopeEntity in the world when used on a block
 * The scope can then be looked through without teleporting the player
 */
public class ScopeItem extends Item {
    
    public ScopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            Direction face = context.getClickedFace();
            
            // Calculate spawn position - grid aligned
            Vec3 clickPos = context.getClickLocation();
            Vec3 spawnPos = gridify(clickPos, 0.5f);
            
            // Offset slightly based on clicked face to avoid being inside the block
            spawnPos = spawnPos.add(
                face.getStepX() * 0.3,
                face.getStepY() * 0.3,
                face.getStepZ() * 0.3
            );
            
            ScopeEntity scope = ModEntities.SCOPE.get().create(level);
            if (scope != null) {
                scope.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                scope.modelItem = context.getItemInHand().copy();
                scope.modelItem.setCount(1);
                
                // Calculate rotation based on face direction
                float yaw;
                float pitch;
                
                if (face == Direction.UP) {
                    pitch = -90; // Point up
                    yaw = context.getRotation(); // Use player facing
                } else if (face == Direction.DOWN) {
                    pitch = 90; // Point down
                    yaw = context.getRotation();
                } else {
                    // Horizontal placement - point in face direction (away from block)
                    pitch = 0;
                    yaw = face.toYRot();
                }
                
                scope.setStoredRotation(yaw, pitch);
                
                level.addFreshEntity(scope);
                context.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Grid-aligns a position to the specified grid size
     */
    public static Vec3 gridify(Vec3 pos, float gridSize) {
        return new Vec3(
            Math.round(pos.x / gridSize) * gridSize,
            Math.round(pos.y / gridSize) * gridSize,
            Math.round(pos.z / gridSize) * gridSize
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cta.scope.place").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cta.scope.use").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.cta.scope.pickup").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
