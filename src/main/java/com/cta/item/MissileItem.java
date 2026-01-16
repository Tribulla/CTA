package com.cta.item;

import com.cta.compat.VSCompat;
import com.cta.entity.MissileEntity;
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
 * MissileItem - Based on Tallyho's MissileItem
 * Places a MissileEntity in the world when used on a block
 * Can be configured as a bomb (drops with gravity) or missile (powered flight)
 */
public class MissileItem extends Item {
    public final String missileId;
    public final boolean isBomb; // True for bombs (gravity drop), false for missiles (powered)
    
    public MissileItem(Properties properties) {
        super(properties);
        this.missileId = "";
        this.isBomb = false;
    }

    public MissileItem(Properties properties, String missileId) {
        super(properties);
        this.missileId = missileId;
        this.isBomb = false;
    }

    public MissileItem(Properties properties, String missileId, boolean isBomb) {
        super(properties);
        this.missileId = missileId;
        this.isBomb = isBomb;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            Direction face = context.getClickedFace();
            BlockPos clickedBlockPos = context.getClickedPos();
            
            // Calculate spawn position - place anywhere on clicked surface
            Vec3 clickPos = context.getClickLocation();
            // Place directly at clicked location, offset slightly away from block
            Vec3 localSpawnPos = clickPos.add(
                face.getStepX() * 0.05,
                face.getStepY() * 0.05,
                face.getStepZ() * 0.05
            );
            
            // Transform spawn position to world coordinates if on a VS ship
            Vec3 worldSpawnPos = VSCompat.toWorldCoordinates(level, clickedBlockPos, localSpawnPos);
            
            MissileEntity missile = ModEntities.MISSILE.get().create(level);
            if (missile != null) {
                missile.setPos(worldSpawnPos.x, worldSpawnPos.y, worldSpawnPos.z);
                missile.modelItem = context.getItemInHand().copy();
                missile.modelItem.setCount(1);
                missile.setIsBomb(this.isBomb);
                
                // Store the block position for VS reference when launching via redstone
                missile.setPlacementBlockPos(clickedBlockPos);
                // Also store the precise ship-local position for smooth ship tracking
                missile.setShipLocalPosition(localSpawnPos);
                
                // Use player's actual look angles (world space)
                float playerYaw = context.getPlayer() != null ? context.getPlayer().getYRot() : context.getRotation();
                float playerPitch = context.getPlayer() != null ? context.getPlayer().getXRot() : 0;

                // Convert world rotation to ship-local for storage
                float localYaw = VSCompat.transformYawToShip(level, clickedBlockPos, playerYaw);
                float localPitch = VSCompat.transformPitchToShip(level, clickedBlockPos, playerYaw, playerPitch);

                // Snap to 6 cardinal directions in ship-local space
                float snappedLocalYaw = snapToNearest90(localYaw);
                float snappedLocalPitch = snapToCardinalPitch(localPitch);

                missile.setShipLocalRotation(snappedLocalYaw, snappedLocalPitch);
                missile.setShipLocalRoll(0.0f); // Roll ignored per user request

                // Compute world rotation from ship-local so initial visual matches ship orientation
                float worldYaw = VSCompat.transformYawToWorld(level, clickedBlockPos, snappedLocalYaw);
                float worldPitch = VSCompat.transformPitchToWorld(level, clickedBlockPos, snappedLocalYaw, snappedLocalPitch);
                missile.setStoredRotation(worldYaw, worldPitch);
                
                level.addFreshEntity(missile);
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
        tooltip.add(Component.translatable("tooltip.cta.missile.place").withStyle(ChatFormatting.GRAY));
        if (isBomb) {
            tooltip.add(Component.translatable("tooltip.cta.bomb.drop").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.cta.missile.launch").withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
