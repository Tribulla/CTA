// broken, needs to be fixed if not even fully remade from scratch

package com.cta.entity;

import com.cta.compat.VSCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * MissileEntity - Based on Tallyho's MountedMissileEntity
 * Can be placed in the world and launched via redstone
 * Missiles fly forward with thrust, bombs just drop with gravity
 */
public class MissileEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_DEPLOYED = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    
    public ItemStack modelItem = ItemStack.EMPTY;
    
    public static final int NOCLIP_TICKS = 5;
    
    protected boolean lastPowered = false;
    protected int ticksSinceLaunch = 0;
    protected int fuel = 120; // Ticks of fuel for missiles (~6 seconds)
    protected float explosionPower = 4.0f;
    protected boolean isBomb = false; // True for bombs (no motor), false for missiles
    protected boolean hasDetonated = false; // Prevent multiple detonations
    
    // Chunk loading tracking
    private ChunkPos lastForcedChunk = null;
    
    // VS ship integration - store placement position for ship velocity calculation
    @Nullable
    protected BlockPos placementBlockPos = null;
    // Precise ship-local position for smooth tracking with moving ships
    @Nullable
    protected Vec3 shipLocalPosition = null;
    // Ship-local rotation for tracking with rotating ships
    protected float shipLocalYaw = 0.0f;
    protected float shipLocalPitch = 0.0f;
    // Ship-local roll for rotating with ship orientation
    protected float shipLocalRoll = 0.0f;

    public MissileEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH, 10.0f);
        this.entityData.define(DATA_DEPLOYED, false);
        this.entityData.define(DATA_YAW, 0.0f);
        this.entityData.define(DATA_PITCH, 0.0f);
        this.entityData.define(DATA_ROLL, 0.0f);
    }

    /**
     * Set the stored rotation (used for rendering and launch direction)
     */
    public void setStoredRotation(float yaw, float pitch) {
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, pitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
    }
    
    /**
     * Set the block position where this missile was placed
     * Used for VS ship velocity calculation when launching
     */
    public void setPlacementBlockPos(@Nullable BlockPos pos) {
        this.placementBlockPos = pos;
        // Also store the precise position for ship tracking
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
    
    /**
     * Get the block position where this missile was placed
     */
    @Nullable
    public BlockPos getPlacementBlockPos() {
        return this.placementBlockPos;
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

    @Override
    public void tick() {
        super.tick();
        
        // Keep entity position and rotation synced when not deployed
        if (!isDeployed()) {
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
                this.setYRot(worldYaw);
                this.setXRot(worldPitch);
                
                // Update rotation snapshot for smooth interpolation
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
            } else {
                // Apply rotation from stored data
                this.setYRot(getStoredYaw());
                this.setXRot(getStoredPitch());
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
            }
        }
        
        if (!this.level().isClientSide) {
            // Check for redstone power to launch (server only)
            // Use VS-aware redstone checking - checks at ship-local position if on a VS ship
            boolean powered = VSCompat.hasRedstoneSignal(this.level(), this.placementBlockPos, this.position());
            if (powered && !lastPowered && !isDeployed()) {
                launch();
            }
            lastPowered = powered;
        }
        
        if (isDeployed()) {
            ticksSinceLaunch++;
            
            // Force-load the chunk we're in so missile doesn't despawn
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                ChunkPos currentChunk = new ChunkPos(this.blockPosition());
                if (lastForcedChunk == null || !lastForcedChunk.equals(currentChunk)) {
                    // Unload previous chunk if we had one
                    if (lastForcedChunk != null) {
                        ForgeChunkManager.forceChunk(serverLevel, "cta", this.blockPosition(), lastForcedChunk.x, lastForcedChunk.z, false, false);
                    }
                    // Force load new chunk
                    ForgeChunkManager.forceChunk(serverLevel, "cta", this.blockPosition(), currentChunk.x, currentChunk.z, true, false);
                    lastForcedChunk = currentChunk;
                }
            }
            
            if (isBomb) {
                // Bombs just fall with gravity
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05, 0));
                // Air resistance
                this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
            } else {
                // Missiles fly forward with thrust based on current rotation
                if (fuel > 0) {
                    // Calculate forward vector from entity rotation
                    float yawRad = (float) Math.toRadians(this.getYRot());
                    float pitchRad = (float) Math.toRadians(this.getXRot());
                    double x = -Math.sin(yawRad) * Math.cos(pitchRad);
                    double y = -Math.sin(pitchRad);
                    double z = Math.cos(yawRad) * Math.cos(pitchRad);
                    Vec3 forward = new Vec3(x, y, z);
                    
                    this.setDeltaMovement(this.getDeltaMovement().add(forward.scale(0.15)));
                    fuel--;
                } else {
                    // Out of fuel - apply gravity
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
                }
                // Air resistance
                this.setDeltaMovement(this.getDeltaMovement().scale(0.99));
            }
            
            // Update rotation to face movement direction
            Vec3 motion = this.getDeltaMovement();
            if (motion.lengthSqr() > 0.0001) {
                double hDist = motion.horizontalDistance();
                // Calculate yaw: atan2(x, z) gives angle where +Z is 0, +X is 90
                // Minecraft yaw: 0 = South (+Z), 90 = West (-X), 180 = North (-Z), 270 = East (+X)
                // So we negate x: atan2(-x, z) or equivalently -atan2(x, z) + adjustment
                float targetYaw = (float) (Math.atan2(-motion.x, motion.z) * (180.0 / Math.PI));
                float targetPitch = (float) (Math.atan2(-motion.y, hDist) * (180.0 / Math.PI));
                
                // Smoothly interpolate rotation
                float yawDiff = targetYaw - this.getYRot();
                while (yawDiff > 180) yawDiff -= 360;
                while (yawDiff < -180) yawDiff += 360;
                
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
                this.setYRot(this.getYRot() + yawDiff * 0.3f);
                this.setXRot(this.getXRot() + (targetPitch - this.getXRot()) * 0.3f);
            }
            
            // Move
            this.move(MoverType.SELF, this.getDeltaMovement());
            
            // Check collision after noclip period (server only)
            if (!this.level().isClientSide && ticksSinceLaunch > NOCLIP_TICKS) {
                if (this.horizontalCollision || this.verticalCollision || this.onGround()) {
                    detonate(this.position());
                }
            }
        }
    }

    /**
     * Launch the missile/bomb
     */
    public boolean launch() {
        if (isDeployed()) return false;
        
        this.entityData.set(DATA_DEPLOYED, true);
        this.ticksSinceLaunch = 0;
        
        // Entity rotation is already set to stored rotation from tick()
        // Calculate initial velocity in the direction the entity is facing
        float yawRad = (float) Math.toRadians(this.getYRot());
        float pitchRad = (float) Math.toRadians(this.getXRot());
        
        // Direction vector from yaw/pitch (Minecraft convention)
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 forward = new Vec3(x, y, z);
        
        if (isBomb) {
            // Bombs start with small forward velocity then fall
            this.setDeltaMovement(forward.scale(0.1));
        } else {
            // Missiles get good initial velocity
            this.setDeltaMovement(forward.scale(0.8));
        }
        
        // Apply ship velocity if launched from a VS ship
        // Uses placementBlockPos if set, otherwise falls back to current position
        BlockPos referencePos = placementBlockPos != null ? placementBlockPos : this.blockPosition();
        VSCompat.applyShipVelocityToEntity(this.level(), referencePos, this);
        
        return true;
    }

    protected void detonate(Vec3 pos) {
        if (!this.level().isClientSide && !hasDetonated) {
            hasDetonated = true;
            // Release forced chunk before discarding
            releaseChunkLoad();
            // Discard FIRST to prevent chain reaction from re-damaging this entity
            this.discard();
            // Then explode
            this.level().explode(null, pos.x, pos.y, pos.z, explosionPower, Level.ExplosionInteraction.TNT);
        }
    }

    /**
     * Release the chunk force-load when missile is removed
     */
    private void releaseChunkLoad() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel && lastForcedChunk != null) {
            ForgeChunkManager.forceChunk(serverLevel, "cta", this.blockPosition(), lastForcedChunk.x, lastForcedChunk.z, false, false);
            lastForcedChunk = null;
        }
    }
    
    @Override
    public void remove(RemovalReason reason) {
        releaseChunkLoad();
        super.remove(reason);
    }

    public boolean isDeployed() {
        return this.entityData.get(DATA_DEPLOYED);
    }

    public void setIsBomb(boolean bomb) {
        this.isBomb = bomb;
        if (bomb) {
            this.fuel = 0; // Bombs have no fuel
        }
    }

    public boolean isBomb() {
        return this.isBomb;
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
                // Pickup missile/bomb
                if (!player.getAbilities().instabuild) {
                    player.getInventory().add(modelItem.copy());
                }
                this.discard();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
    
    /**
     * Check if item is Create mod's wrench
     */
    private boolean isCreateWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Check by registry name to avoid hard dependency on Create
        var registryName = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName == null) return false;
        return registryName.getNamespace().equals("create") && registryName.getPath().equals("wrench");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || hasDetonated) return false;
        
        float health = this.entityData.get(DATA_HEALTH);
        health -= amount;
        this.entityData.set(DATA_HEALTH, health);
        
        if (health <= 0) {
            detonate(this.position());
            return true;
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isDeployed();
    }

    @Override
    public boolean isNoGravity() {
        // No gravity when not deployed - stays in place
        return !isDeployed();
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5f;
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
        this.entityData.set(DATA_DEPLOYED, compound.getBoolean("Deployed"));
        this.entityData.set(DATA_HEALTH, compound.getFloat("Health"));
        this.entityData.set(DATA_YAW, compound.getFloat("StoredYaw"));
        this.entityData.set(DATA_PITCH, compound.getFloat("StoredPitch"));
        this.fuel = compound.getInt("Fuel");
        this.ticksSinceLaunch = compound.getInt("TicksSinceLaunch");
        this.isBomb = compound.getBoolean("IsBomb");
        if (compound.contains("ModelItem")) {
            this.modelItem = ItemStack.of(compound.getCompound("ModelItem"));
        }
        // Load placement position for VS ship velocity calculation
        if (compound.contains("PlacementX")) {
            this.placementBlockPos = new BlockPos(
                compound.getInt("PlacementX"),
                compound.getInt("PlacementY"),
                compound.getInt("PlacementZ")
            );
        }
        // Load ship-local position for smooth ship tracking
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
        compound.putBoolean("Deployed", this.entityData.get(DATA_DEPLOYED));
        compound.putFloat("Health", this.entityData.get(DATA_HEALTH));
        compound.putFloat("StoredYaw", this.entityData.get(DATA_YAW));
        compound.putFloat("StoredPitch", this.entityData.get(DATA_PITCH));
        compound.putInt("Fuel", this.fuel);
        compound.putInt("TicksSinceLaunch", this.ticksSinceLaunch);
        compound.putBoolean("IsBomb", this.isBomb);
        compound.put("ModelItem", this.modelItem.save(new CompoundTag()));
        // Save placement position for VS ship velocity calculation
        if (this.placementBlockPos != null) {
            compound.putInt("PlacementX", this.placementBlockPos.getX());
            compound.putInt("PlacementY", this.placementBlockPos.getY());
            compound.putInt("PlacementZ", this.placementBlockPos.getZ());
        }
        // Save ship-local position for smooth ship tracking
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
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeItem(this.modelItem);
        buffer.writeBoolean(this.entityData.get(DATA_DEPLOYED));
        buffer.writeFloat(this.entityData.get(DATA_YAW));
        buffer.writeFloat(this.entityData.get(DATA_PITCH));
        buffer.writeBoolean(this.isBomb);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.modelItem = buffer.readItem();
        this.entityData.set(DATA_DEPLOYED, buffer.readBoolean());
        this.entityData.set(DATA_YAW, buffer.readFloat());
        this.entityData.set(DATA_PITCH, buffer.readFloat());
        this.isBomb = buffer.readBoolean();
        // Apply rotation immediately
        this.setYRot(getStoredYaw());
        this.setXRot(getStoredPitch());
    }

    public ItemStack getModelItem() {
        return this.modelItem;
    }

    public int getTicksSinceLaunch() {
        return this.ticksSinceLaunch;
    }
    
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        // When attached to a ship and not deployed, skip client-side interpolation
        // Server will teleport us to the correct position each tick via ship tracking
        if (!isDeployed() && shipLocalPosition != null && placementBlockPos != null) {
            this.setPos(x, y, z);
            this.setYRot(yaw);
            this.setXRot(pitch);
        } else {
            // Normal lerp behavior for deployed missiles
            super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
        }
    }
}
