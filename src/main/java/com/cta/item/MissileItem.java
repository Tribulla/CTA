package com.cta.item;

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
            
            // Calculate spawn position - grid aligned like Tallyho
            Vec3 clickPos = context.getClickLocation();
            Vec3 spawnPos = gridify(clickPos, 0.5f);
            
            // Offset slightly based on clicked face to avoid being inside the block
            spawnPos = spawnPos.add(
                face.getStepX() * 0.3,
                face.getStepY() * 0.3,
                face.getStepZ() * 0.3
            );
            
            MissileEntity missile = ModEntities.MISSILE.get().create(level);
            if (missile != null) {
                missile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                missile.modelItem = context.getItemInHand().copy();
                missile.modelItem.setCount(1);
                missile.setIsBomb(this.isBomb);
                
                // Calculate rotation based on face direction
                float yaw;
                float pitch;
                
                if (face == Direction.UP) {
                    pitch = -90; // Point up
                    // Use player's opposite facing so missile points away
                    yaw = context.getRotation();
                } else if (face == Direction.DOWN) {
                    pitch = 90; // Point down
                    yaw = context.getRotation();
                } else {
                    // Horizontal placement - point in face direction (away from block)
                    pitch = 0;
                    yaw = face.toYRot();
                }
                
                // Use the new setStoredRotation method to properly sync
                missile.setStoredRotation(yaw, pitch);
                
                level.addFreshEntity(missile);
                context.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Grid-aligns a position to the specified grid size
     * Like Tallyho's gridify method
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
        tooltip.add(Component.translatable("tooltip.cta.missile.place").withStyle(ChatFormatting.GRAY));
        if (isBomb) {
            tooltip.add(Component.translatable("tooltip.cta.bomb.drop").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.cta.missile.launch").withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
