package com.cta.entity;

import com.cta.utils.AngleLimits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * ScopeEntity - Placeable scope camera that doesn't require a block
 * Players can look through it without being teleported
 */
public class ScopeEntity extends CameraEntity {
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(ScopeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(ScopeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(ScopeEntity.class, EntityDataSerializers.FLOAT);
    
    public ItemStack modelItem = ItemStack.EMPTY;
    
    public ScopeEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        // Default angle limits for scope - can look around quite a bit
        setParams(0, new AngleLimits(180, 90, 0), 30);
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_YAW, 0.0f);
        this.entityData.define(DATA_PITCH, 0.0f);
        this.entityData.define(DATA_HEALTH, 10.0f);
    }

    /**
     * Set the stored rotation for rendering
     */
    public void setStoredRotation(float yaw, float pitch) {
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, pitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
        // Update base yaw for angle limits
        setParams(yaw, new AngleLimits(180, 90, 0), 30);
    }

    public float getStoredYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public float getStoredPitch() {
        return this.entityData.get(DATA_PITCH);
    }

    @Override
    public void tick() {
        super.tick();
        // Scope stays in place - no movement needed
    }

    @Override
    protected boolean isShortLived() {
        return false; // Scope is permanent until picked up or destroyed
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            // Check if player is holding Create wrench first
            ItemStack heldItem = player.getItemInHand(hand);
            if (isCreateWrench(heldItem)) {
                // Rotate by 15 degrees (shift for reverse)
                float delta = player.isShiftKeyDown() ? -15 : 15;
                float newYaw = getStoredYaw() + delta;
                if (newYaw >= 360) newYaw -= 360;
                if (newYaw < 0) newYaw += 360;
                setStoredRotation(newYaw, getStoredPitch());
                return InteractionResult.SUCCESS;
            }
            
            if (player.isShiftKeyDown()) {
                // Pickup scope
                if (!player.getAbilities().instabuild) {
                    player.getInventory().add(modelItem.isEmpty() ? 
                        new ItemStack(com.cta.registry.ModItems.PANTHER_SCOPE.get()) : modelItem.copy());
                }
                this.discard();
                return InteractionResult.SUCCESS;
            } else if (player instanceof ServerPlayer serverPlayer) {
                // Start viewing through scope
                if (isPossessed()) {
                    // Someone else is using it
                    return InteractionResult.FAIL;
                }
                // Set camera rotation to match scope direction before viewing
                this.setYRot(getStoredYaw());
                this.setXRot(getStoredPitch());
                startViewing(serverPlayer);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    /**
     * Check if item is Create mod's wrench
     */
    private boolean isCreateWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var registryName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName == null) return false;
        return registryName.getNamespace().equals("create") && registryName.getPath().equals("wrench");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        
        float health = this.entityData.get(DATA_HEALTH);
        health -= amount;
        this.entityData.set(DATA_HEALTH, health);
        
        if (health <= 0) {
            // Stop viewing if someone is using it
            if (currentlyViewing.get() != null) {
                stopViewing(currentlyViewing.get());
            }
            this.discard();
            return true;
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true; // Scope stays in place
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5f; // Camera at center of entity
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.5f, 0.5f);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(1.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_YAW, compound.getFloat("StoredYaw"));
        this.entityData.set(DATA_PITCH, compound.getFloat("StoredPitch"));
        this.entityData.set(DATA_HEALTH, compound.getFloat("Health"));
        if (compound.contains("ModelItem")) {
            this.modelItem = ItemStack.of(compound.getCompound("ModelItem"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("StoredYaw", this.entityData.get(DATA_YAW));
        compound.putFloat("StoredPitch", this.entityData.get(DATA_PITCH));
        compound.putFloat("Health", this.entityData.get(DATA_HEALTH));
        compound.put("ModelItem", this.modelItem.save(new CompoundTag()));
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeFloat(this.entityData.get(DATA_YAW));
        buffer.writeFloat(this.entityData.get(DATA_PITCH));
        buffer.writeItem(this.modelItem);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        super.readSpawnData(buffer);
        this.entityData.set(DATA_YAW, buffer.readFloat());
        this.entityData.set(DATA_PITCH, buffer.readFloat());
        this.modelItem = buffer.readItem();
        // Apply rotation
        this.setYRot(getStoredYaw());
        this.setXRot(getStoredPitch());
    }

    public ItemStack getModelItem() {
        return this.modelItem;
    }
}
