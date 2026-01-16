package com.cta.entity;

import com.cta.compat.VSCompat;
import com.cta.utils.AngleLimits;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * ScopeEntity - Placeable scope camera that doesn't require a block
 * Players can look through it without being teleported
 */
public class ScopeEntity extends CameraEntity {
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(ScopeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(ScopeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL = SynchedEntityData.defineId(ScopeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(ScopeEntity.class, EntityDataSerializers.FLOAT);
    
    public ItemStack modelItem = ItemStack.EMPTY;
    
    // VS ship integration - track position on ship
    @Nullable
    protected BlockPos placementBlockPos = null;
    @Nullable
    protected Vec3 shipLocalPosition = null;
    // Ship-local rotation for tracking with rotating ships
    protected float shipLocalYaw = 0.0f;
    protected float shipLocalPitch = 0.0f;
    // Ship-local roll for rotating with ship orientation
    protected float shipLocalRoll = 0.0f;
    
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
        this.entityData.define(DATA_ROLL, 0.0f);
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
    
    public float getStoredRoll() {
        return this.entityData.get(DATA_ROLL);
    }
    
    /**
     * Set the block position where this scope was placed
     * Used for VS ship tracking
     */
    public void setPlacementBlockPos(@Nullable BlockPos pos) {
        this.placementBlockPos = pos;
        if (pos != null) {
            this.shipLocalPosition = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }
    
    /**
     * Set the precise ship-local position for smooth ship tracking
     */
    public void setShipLocalPosition(@Nullable Vec3 pos) {
        this.shipLocalPosition = pos;
    }
    
    /**
     * Set the ship-local rotation (rotation relative to ship, not world)
     */
    public void setShipLocalRotation(float yaw, float pitch) {
        this.shipLocalYaw = yaw;
        this.shipLocalPitch = pitch;
    }
    
    /**
     * Set the ship-local roll (barrel roll relative to ship)
     */
    public void setShipLocalRoll(float roll) {
        this.shipLocalRoll = roll;
    }

    @Override
    public void tick() {
        super.tick();
        
        // Update position and rotation to track with VS ship movement - do this EVERY TICK
        if (shipLocalPosition != null && placementBlockPos != null) {
            // Get ship velocity to move with the ship
            Vec3 shipVelocity = VSCompat.getShipVelocity(this.level(), placementBlockPos);
            
            // Update world position from ship-local position
            VSCompat.updateEntityPositionOnShip(this, placementBlockPos, shipLocalPosition);
            
            // Match the ship's velocity so we move with it
            this.setDeltaMovement(shipVelocity);
            
            // Transform ship-local rotation to world rotation
            float worldYaw = VSCompat.transformYawToWorld(this.level(), placementBlockPos, shipLocalYaw);
            float worldPitch = VSCompat.transformPitchToWorld(this.level(), placementBlockPos, shipLocalYaw, shipLocalPitch);
            float worldRoll = VSCompat.transformRollToWorld(this.level(), placementBlockPos, shipLocalRoll);
            this.entityData.set(DATA_YAW, worldYaw);
            this.entityData.set(DATA_PITCH, worldPitch);
            this.entityData.set(DATA_ROLL, worldRoll);
            
            // Apply transformed rotation
            this.setYRot(worldYaw);
            this.setXRot(worldPitch);
            
            // Update rotation snapshot for smooth interpolation
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
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
                // Shift+wrench = rotate pitch, normal wrench = rotate yaw
                float delta = 15;
                if (player.isShiftKeyDown()) {
                    // Rotate pitch
                    float newPitch = getStoredPitch() + delta;
                    // Clamp pitch to -90 to 90
                    if (newPitch > 90) newPitch = -90 + (newPitch - 90);
                    if (newPitch < -90) newPitch = 90 + (newPitch + 90);
                    setStoredRotation(getStoredYaw(), newPitch);
                    // Update ship-local rotation too
                    this.shipLocalPitch = newPitch;
                } else {
                    // Rotate yaw
                    float newYaw = getStoredYaw() + delta;
                    if (newYaw >= 360) newYaw -= 360;
                    if (newYaw < 0) newYaw += 360;
                    setStoredRotation(newYaw, getStoredPitch());
                    // Update ship-local rotation too
                    this.shipLocalYaw = newYaw;
                }
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
        // Load VS ship tracking data
        if (compound.contains("PlacementX")) {
            this.placementBlockPos = new BlockPos(
                compound.getInt("PlacementX"),
                compound.getInt("PlacementY"),
                compound.getInt("PlacementZ")
            );
        }
        if (compound.contains("ShipLocalX")) {
            this.shipLocalPosition = new Vec3(
                compound.getDouble("ShipLocalX"),
                compound.getDouble("ShipLocalY"),
                compound.getDouble("ShipLocalZ")
            );
        }
        // Load ship-local rotation for ship tracking
        this.shipLocalYaw = compound.getFloat("ShipLocalYaw");
        this.shipLocalPitch = compound.getFloat("ShipLocalPitch");
        this.shipLocalRoll = compound.getFloat("ShipLocalRoll");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("StoredYaw", this.entityData.get(DATA_YAW));
        compound.putFloat("StoredPitch", this.entityData.get(DATA_PITCH));
        compound.putFloat("Health", this.entityData.get(DATA_HEALTH));
        compound.put("ModelItem", this.modelItem.save(new CompoundTag()));
        // Save VS ship tracking data
        if (this.placementBlockPos != null) {
            compound.putInt("PlacementX", this.placementBlockPos.getX());
            compound.putInt("PlacementY", this.placementBlockPos.getY());
            compound.putInt("PlacementZ", this.placementBlockPos.getZ());
        }
        if (this.shipLocalPosition != null) {
            compound.putDouble("ShipLocalX", this.shipLocalPosition.x);
            compound.putDouble("ShipLocalY", this.shipLocalPosition.y);
            compound.putDouble("ShipLocalZ", this.shipLocalPosition.z);
        }
        // Save ship-local rotation for ship tracking
        compound.putFloat("ShipLocalYaw", this.shipLocalYaw);
        compound.putFloat("ShipLocalPitch", this.shipLocalPitch);
        compound.putFloat("ShipLocalRoll", this.shipLocalRoll);
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
    
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        // When attached to a ship, skip client-side interpolation
        // Server will teleport us to the correct position each tick via ship tracking
        if (shipLocalPosition != null && placementBlockPos != null) {
            this.setPos(x, y, z);
            this.setYRot(yaw);
            this.setXRot(pitch);
        } else {
            // Normal lerp behavior when not on a ship
            super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
        }
    }
}
