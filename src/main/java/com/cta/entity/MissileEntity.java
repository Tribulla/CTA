// broken, needs to be fixed if not even fully remade from scratch

package com.cta.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

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
    
    public ItemStack modelItem = ItemStack.EMPTY;
    
    public static final int NOCLIP_TICKS = 5;
    
    protected boolean lastPowered = false;
    protected int ticksSinceLaunch = 0;
    protected int fuel = 400; // Ticks of fuel for missiles
    protected float explosionPower = 4.0f;
    protected boolean isBomb = false; // True for bombs (no motor), false for missiles
    protected boolean hasDetonated = false; // Prevent multiple detonations

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

    public float getStoredYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public float getStoredPitch() {
        return this.entityData.get(DATA_PITCH);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Keep entity rotation synced with stored rotation when not deployed
        if (!isDeployed()) {
            this.setYRot(getStoredYaw());
            this.setXRot(getStoredPitch());
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        
        if (!this.level().isClientSide) {
            // Check for redstone power to launch (server only)
            BlockPos pos = this.blockPosition();
            boolean powered = this.level().hasNeighborSignal(pos);
            if (powered && !lastPowered && !isDeployed()) {
                launch();
            }
            lastPowered = powered;
        }
        
        if (isDeployed()) {
            ticksSinceLaunch++;
            
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
                    
                    this.setDeltaMovement(this.getDeltaMovement().add(forward.scale(0.08)));
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
            this.setDeltaMovement(forward.scale(0.5));
        }
        
        return true;
    }

    protected void detonate(Vec3 pos) {
        if (!this.level().isClientSide && !hasDetonated) {
            hasDetonated = true;
            // Discard FIRST to prevent chain reaction from re-damaging this entity
            this.discard();
            // Then explode
            this.level().explode(null, pos.x, pos.y, pos.z, explosionPower, Level.ExplosionInteraction.TNT);
        }
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
                // Rotate by 15 degrees (shift for reverse)
                float delta = player.isShiftKeyDown() ? -15 : 15;
                float newYaw = getStoredYaw() + delta;
                if (newYaw >= 360) newYaw -= 360;
                if (newYaw < 0) newYaw += 360;
                setStoredRotation(newYaw, getStoredPitch());
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
}
