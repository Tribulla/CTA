package com.cta.entity;

import com.cta.network.PacketHandler;
import com.cta.network.SetCameraViewPacket;
import com.cta.utils.AngleLimits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * CameraEntity - Based on Tallyho's CameraEntity2
 * Abstract base class for camera entities that players can view through
 */
public abstract class CameraEntity extends Entity implements IEntityAdditionalSpawnData {
    private float BASE_YAW = 0;
    private AngleLimits ANGLE_LIMIT = new AngleLimits(0, 0, 0);
    private int FOV = 70;
    
    public static final List<net.minecraft.world.entity.player.Player> RECENTLY_DISMOUNTED_PLAYERS = new ArrayList<>();
    
    protected WeakReference<ServerPlayer> currentlyViewing = new WeakReference<>(null);
    protected int timeout = 0;
    
    // Store player's original position to restore when exiting camera view
    protected double storedPlayerX, storedPlayerY, storedPlayerZ;
    protected float storedPlayerYaw, storedPlayerPitch;

    public CameraEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static boolean hasRecentlyDismounted(net.minecraft.world.entity.player.Player player) {
        return RECENTLY_DISMOUNTED_PLAYERS.contains(player);
    }

    protected void setParams(float baseYaw, AngleLimits angleLimits, int fov) {
        this.BASE_YAW = baseYaw;
        this.ANGLE_LIMIT = angleLimits;
        this.FOV = fov;
    }

    public boolean startViewing(ServerPlayer player) {
        if (player.level() != this.level()) return false;
        
        // Clear any previous viewer
        ServerPlayer oldViewer = this.currentlyViewing.get();
        if (oldViewer != null && oldViewer != player) {
            stopViewing(oldViewer);
        }
        
        this.currentlyViewing = new WeakReference<>(player);
        
        // Store player's original position to restore when exiting
        this.storedPlayerX = player.getX();
        this.storedPlayerY = player.getY();
        this.storedPlayerZ = player.getZ();
        this.storedPlayerYaw = player.getYRot();
        this.storedPlayerPitch = player.getXRot();
        
        // Set camera entity's rotation to the stored/base yaw so view starts correctly oriented
        // This prevents looking straight up when entering scope
        this.setYRot(BASE_YAW);
        this.setXRot(0); // Start with level pitch (can be adjusted by subclasses)
        
        player.setCamera(this);
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SetCameraViewPacket(this));
        this.timeout = 0;
        return true;
    }

    public void stopViewing(ServerPlayer player) {
        if (this.currentlyViewing.get() == player) {
            this.currentlyViewing.clear();
            
            // Restore player's original position before releasing camera
            player.teleportTo(this.storedPlayerX, this.storedPlayerY, this.storedPlayerZ);
            player.setYRot(this.storedPlayerYaw);
            player.setXRot(this.storedPlayerPitch);
            
            player.setCamera(player);
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SetCameraViewPacket(player));
            
            // Track recently dismounted
            RECENTLY_DISMOUNTED_PLAYERS.add(player);
            // Remove after a short time (handled elsewhere or schedule removal)
        }
        
        // Remove short-lived camera entities
        if (isShortLived()) {
            this.discard();
        }
    }

    protected boolean isShortLived() {
        return true; // Default: camera entities are removed when player stops viewing
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            ServerPlayer player = currentlyViewing.get();
            if (player != null) {
                // Check if player should stop viewing
                if (player.isDeadOrDying() || player.level() != this.level() || player.isSpectator()) {
                    stopViewing(player);
                    return;
                }
                
                // Distance check - stop if too far
                if (player.distanceToSqr(this) > 128 * 128) {
                    stopViewing(player);
                    return;
                }
                
                timeout = 0;
            } else if (isShortLived()) {
                // No viewer and short-lived - increment timeout
                timeout++;
                if (timeout > 100) { // 5 seconds
                    this.discard();
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        // No synched data needed
    }

    /**
     * Turn the camera view within limits
     */
    public void turnView(double yaw, double pitch) {
        turnView(yaw, pitch, true);
    }

    public void turnView(double yaw, double pitch, boolean applyLimits) {
        float newYaw = this.getYRot() + (float) yaw;
        float newPitch = this.getXRot() + (float) pitch;
        
        if (applyLimits && ANGLE_LIMIT != null) {
            // Apply yaw limits relative to base
            float relativeYaw = newYaw - BASE_YAW;
            relativeYaw = Math.max(-ANGLE_LIMIT.maxYaw(), Math.min(ANGLE_LIMIT.maxYaw(), relativeYaw));
            newYaw = BASE_YAW + relativeYaw;
            
            // Apply pitch limits
            newPitch = Math.max(-ANGLE_LIMIT.maxPitch(), Math.min(ANGLE_LIMIT.maxPitch(), newPitch));
        }
        
        this.setYRot(newYaw);
        this.setXRot(newPitch);
    }

    public int getFOV() {
        return FOV;
    }

    public AngleLimits getRotationLimit() {
        return ANGLE_LIMIT;
    }

    public float getBaseYaw() {
        return BASE_YAW;
    }

    public boolean isPossessed() {
        return currentlyViewing.get() != null;
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.0f;
    }

    @Override
    public double getMyRidingOffset() {
        return 0.0;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    protected Vec2 getRotationFromDirection(Vec3 direction) {
        double yaw = Math.atan2(direction.x, direction.z) * (180.0 / Math.PI);
        double pitch = Math.atan2(direction.y, direction.horizontalDistance()) * (-180.0 / Math.PI);
        return new Vec2((float) pitch, (float) yaw);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // Can be extended by subclasses
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // Can be extended by subclasses
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeFloat(BASE_YAW);
        buffer.writeInt(FOV);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.BASE_YAW = buffer.readFloat();
        this.FOV = buffer.readInt();
    }
}
