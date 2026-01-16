package com.cta.item;

import com.cta.compat.VSCompat;
import com.cta.entity.ScopeEntity;
import com.cta.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
            
            // Calculate spawn position - place anywhere on clicked surface
            Vec3 clickPos = context.getClickLocation();
            // Place directly at clicked location, offset slightly away from block
            Vec3 spawnPos = clickPos.add(
                face.getStepX() * 0.05,
                face.getStepY() * 0.05,
                face.getStepZ() * 0.05
            );
            
            ScopeEntity scope = ModEntities.SCOPE.get().create(level);
            if (scope != null) {
                // Transform spawn position to world coordinates if on a VS ship
                BlockPos clickedBlockPos = context.getClickedPos();
                Vec3 worldSpawnPos = VSCompat.toWorldCoordinates(level, clickedBlockPos, spawnPos);
                
                scope.setPos(worldSpawnPos.x, worldSpawnPos.y, worldSpawnPos.z);
                scope.modelItem = context.getItemInHand().copy();
                scope.modelItem.setCount(1);
                
                // Store the clicked block position for VS ship tracking
                scope.setPlacementBlockPos(clickedBlockPos);
                scope.setShipLocalPosition(spawnPos); // Store precise ship-local position for smooth tracking
                
                // Use player's actual look angles (world space)
                float playerYaw = context.getPlayer() != null ? context.getPlayer().getYRot() : context.getRotation();
                float playerPitch = context.getPlayer() != null ? context.getPlayer().getXRot() : 0;

                // Convert world rotation to ship-local for storage
                float localYaw = VSCompat.transformYawToShip(level, clickedBlockPos, playerYaw);
                float localPitch = VSCompat.transformPitchToShip(level, clickedBlockPos, playerYaw, playerPitch);

                // Snap to 6 cardinal directions in ship-local space
                float snappedLocalYaw = snapToNearest90(localYaw);
                float snappedLocalPitch = snapToCardinalPitch(localPitch);

                scope.setShipLocalRotation(snappedLocalYaw, snappedLocalPitch);
                scope.setShipLocalRoll(0.0f); // Roll ignored per user request

                // Compute world rotation from ship-local so initial visual matches ship orientation
                float worldYaw = VSCompat.transformYawToWorld(level, clickedBlockPos, snappedLocalYaw);
                float worldPitch = VSCompat.transformPitchToWorld(level, clickedBlockPos, snappedLocalYaw, snappedLocalPitch);
                scope.setStoredRotation(worldYaw, worldPitch);
                
                level.addFreshEntity(scope);
                context.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Snaps a pitch angle to cardinal directions: 0 (horizontal), 90 (up), or -90 (down)
     */
    private static float snapToCardinalPitch(float pitch) {
        // Normalize pitch to -90 to 90 range
        while (pitch > 90) pitch -= 180;
        while (pitch < -90) pitch += 180;
        
        // Snap to nearest cardinal: down (-90), horizontal (0), or up (90)
        if (pitch < -45) return -90;  // Down
        if (pitch > 45) return 90;    // Up
        return 0;                      // Horizontal
    }

    /**
     * Snaps an angle to the nearest 90-degree increment (0, 90, 180, 270)
     */
    private static float snapToNearest90(float angle) {
        // Normalize to 0-360
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;
        
        // Snap to nearest 90
        if (angle < 45 || angle >= 315) return 0;      // South
        if (angle < 135) return 90;                     // West
        if (angle < 225) return 180;                    // North
        return 270;                                     // East
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cta.scope.place").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cta.scope.use").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.cta.scope.pickup").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
