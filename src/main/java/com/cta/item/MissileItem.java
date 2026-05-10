package com.cta.item;

import com.cta.compat.VSCompat;
import com.cta.entity.MissileEntity;
import com.cta.entity.MissileEntity.WarheadType;
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


public class MissileItem extends Item {
    public final String missileId;
    public final boolean isBomb;
    public final WarheadType warheadType;
    private static final Vec3 HITBOX = new Vec3(0.5, 0.5, 0.5);
    
    public MissileItem(Properties properties) {
        super(properties);
        this.missileId = "";
        this.isBomb = false;
        this.warheadType = WarheadType.HE;
    }

    public MissileItem(Properties properties, String missileId) {
        super(properties);
        this.missileId = missileId;
        this.isBomb = false;
        this.warheadType = WarheadType.HE;
    }

    public MissileItem(Properties properties, String missileId, boolean isBomb) {
        super(properties);
        this.missileId = missileId;
        this.isBomb = isBomb;
        this.warheadType = WarheadType.HE;
    }
    
    public MissileItem(Properties properties, String missileId, boolean isBomb, WarheadType warheadType) {
        super(properties);
        this.missileId = missileId;
        this.isBomb = isBomb;
        this.warheadType = warheadType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            Direction face = context.getClickedFace();
            BlockPos clickedBlockPos = context.getClickedPos();
            
            Vec3 clickPos = context.getClickLocation();
            Vec3 localSpawnPos = clickPos.add(
                face.getStepX() * (HITBOX.x / 2),
                face.getStepY() * (HITBOX.y / 2) - HITBOX.y / 2,
                face.getStepZ() * (HITBOX.z / 2)
            );

            MissileEntity missile = ModEntities.MISSILE.get().create(level);
            if (missile != null) {
                missile.setPos(localSpawnPos.x, localSpawnPos.y, localSpawnPos.z);
                missile.modelItem = context.getItemInHand().copy();
                missile.modelItem.setCount(1);
                
                missile.setMissileId(this.missileId, this.warheadType);
                
                missile.setPlacementBlockPos(clickedBlockPos);
                missile.setShipLocalPosition(localSpawnPos);
                missile.setAttachedToShip(VSCompat.isOnShip(level, clickedBlockPos));
                
                float playerYaw = context.getPlayer() != null ? context.getPlayer().getYRot() : context.getRotation();
                float playerPitch = context.getPlayer() != null ? context.getPlayer().getXRot() : 0;

                float localYaw = VSCompat.transformYawToShip(level, clickedBlockPos, playerYaw);
                float localPitch = VSCompat.transformPitchToShip(level, clickedBlockPos, playerYaw, playerPitch);

                float snappedLocalYaw = snapToNearest90(localYaw);
                float snappedLocalPitch = snapToCardinalPitch(localPitch);
                if (snappedLocalPitch < 0)
                    snappedLocalYaw = (snappedLocalYaw + 180) % 360;

                missile.setShipLocalRotation(snappedLocalYaw, snappedLocalPitch);
                missile.setShipLocalRoll(0.0f);

                missile.setStoredRotation(snappedLocalYaw, snappedLocalPitch);
                
                level.addFreshEntity(missile);
                context.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static float snapToCardinalPitch(float pitch) {
        while (pitch > 90) pitch -= 180;
        while (pitch < -90) pitch += 180;
        
        if (pitch < -45) return -90;
        if (pitch > 45) return 90;
        return 0;
    }

    private static float snapToNearest90(float angle) {
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;
        
        if (angle < 45 || angle >= 315) return 0;
        if (angle < 135) return 90;
        if (angle < 225) return 180;
        return 270;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cta.missile.place").withStyle(ChatFormatting.GRAY));
        
        switch (warheadType) {
            case HE:
                tooltip.add(Component.literal("HE - High Explosive").withStyle(ChatFormatting.RED));
                break;
            case HEAT:
                tooltip.add(Component.literal("HEAT - Armor Penetrating").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("Passes through armor, detonates behind").withStyle(ChatFormatting.DARK_GRAY));
                break;
            case HEFRAG:
                tooltip.add(Component.literal("HEFRAG - Fragmentation").withStyle(ChatFormatting.LIGHT_PURPLE));
                tooltip.add(Component.literal("Explosion + fragment spray").withStyle(ChatFormatting.DARK_GRAY));
                break;
        }
        
        if (isBomb) {
            tooltip.add(Component.translatable("tooltip.cta.bomb.drop").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.cta.missile.launch").withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
