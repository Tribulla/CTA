package com.cta.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.Tribulla.thermodynamica.api.HeatAPI;
import com.Tribulla.thermodynamica.api.targeting.HeatTargeting;
import com.cta.client.debug.MissileDebugRenderer;
import com.cta.client.debug.MissileDebugRenderer.DebugColor;
import com.cta.command.CTADebugCommand;
import com.cta.compat.CBCCompat;
import com.cta.compat.CBCIntegration;
import com.cta.compat.VSCompat;
import com.cta.config.MissileConfig;
import com.cta.config.MissileConfig.MissileCategory;
import com.cta.config.MissileConfig.MissileTypeConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class MissileEntity extends Entity implements IEntityAdditionalSpawnData {
    
    public enum WarheadType {
        HE,
        HEAT,
        HEFRAG
    }
    
    public enum GuidanceType {
        NONE,
        HEAT_SEEKING
    }
    
    public enum SteeringMethod {
        PURE_PURSUIT,
        PROPORTIONAL_NAV,
        LEAD_PURSUIT,
        COMMAND_LOS
    }
    
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_DEPLOYED = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_MISSILE_ID = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_WARHEAD_TYPE = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ATTACHED_TO_SHIP = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.BOOLEAN);
    
    public ItemStack modelItem = ItemStack.EMPTY;
    
    public static final int NOCLIP_TICKS = 5;
    
    protected boolean lastPowered = false;
    protected int ticksSinceLaunch = 0;
    protected int fuel = 120;
    protected float explosionPower = 4.0f;
    protected float baseExplosionPower = 4.0f;
    protected boolean isBomb = false;
    protected boolean hasDetonated = false;
    
    protected String missileId = "";
    protected WarheadType warheadType = WarheadType.HE;
    protected ItemStack fuze = ItemStack.EMPTY;
    protected double thrust = 0.15;
    protected double gravity = 0.04;
    protected double airResistance = 0.99;
    protected double initialSpeed = 0.8;
    protected double dropRate = 0.0;
    protected boolean hasAirbrake = false;
    protected double glideRatio = 0.0;
    protected MissileCategory category = MissileCategory.MISSILE;
    
    protected GuidanceType guidanceType = GuidanceType.NONE;
    protected SteeringMethod steeringMethod = SteeringMethod.PROPORTIONAL_NAV;
    protected double seekerFov = 30.0;
    protected double seekerRange = 2048.0;
    protected double seekerMinTemp = 30.0;
    protected double turnRate = 4.0;
    protected double navGain = 4.0;
    protected int seekerCooldown = 0;
    protected Vec3 currentTargetPos = null;
    protected BlockPos currentTargetBlockPos = null;
    protected boolean isCurrentTargetOnShip = false;
    protected int targetLostTicks = 0;
    protected int noGuidanceTicks = 0;
    
    protected Vec3 launchWorldPos = null;
    
    protected Vec3 previousTargetPos = null;
    protected Vec3 smoothedTargetVel = null;
    protected Vec3 previousSmoothedVel = null;
    protected Vec3 smoothedTargetAccel = null;
    protected int trackingTicks = 0;
    
    protected double armorPenetration = 5.0;
    protected final Set<BlockPos> penetratedBlocks = new HashSet<>();
    
    protected int fragCount = 20;
    protected double fragDamage = 4.0;
    protected double fragRange = 10.0;
    
    protected ChunkPos lastForcedChunk = null;
    
    @Nullable
    protected BlockPos placementBlockPos = null;
    @Nullable
    protected Vec3 shipLocalPosition = null;
    protected float shipLocalYaw = 0.0f;
    protected float shipLocalPitch = 0.0f;
    protected float shipLocalRoll = 0.0f;
    
    protected boolean hasImpacted = false;
    protected int impactDelayTicks = 0;

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
        this.entityData.define(DATA_MISSILE_ID, "");
        this.entityData.define(DATA_WARHEAD_TYPE, 0);
        this.entityData.define(DATA_ATTACHED_TO_SHIP, false);
    }
    
    public void setMissileId(String id, WarheadType warhead) {
        this.missileId = id;
        this.warheadType = warhead;
        this.entityData.set(DATA_MISSILE_ID, id);
        this.entityData.set(DATA_WARHEAD_TYPE, warhead.ordinal());
        loadConfigValues();
    }
    
    public void setMissileId(String id) {
        setMissileId(id, WarheadType.HE);
    }
    
    public WarheadType getWarheadType() {
        return warheadType;
    }
    
    public void setFuze(ItemStack fuze) {
        this.fuze = fuze == null || fuze.isEmpty() ? ItemStack.EMPTY : fuze.copy();
    }
    
    public ItemStack getFuze() {
        return this.fuze;
    }
    
    public boolean hasFuze() {
        return !this.fuze.isEmpty() && CBCCompat.isFuzeItem(this.fuze);
    }
    
    public boolean isBomb() {
        return this.isBomb;
    }
    
    protected void loadConfigValues() {
        if (missileId == null || missileId.isEmpty()) return;
        
        MissileTypeConfig config = MissileConfig.getConfig(missileId);
        this.category = config.category;
        this.fuel = config.getFuelTicks();
        this.thrust = config.getThrust();
        this.baseExplosionPower = config.getExplosionPower();
        this.explosionPower = this.baseExplosionPower;
        this.gravity = config.getGravity();
        this.airResistance = config.getAirResistance();
        this.initialSpeed = config.getInitialSpeed();
        this.dropRate = config.getDropRate();
        this.hasAirbrake = config.hasAirbrake();
        this.glideRatio = config.getGlideRatio();
        this.isBomb = (fuel == 0);
        
        this.guidanceType = config.getGuidanceType();
        this.steeringMethod = config.getSteeringMethod();
        this.seekerFov = config.getSeekerFov();
        this.seekerRange = config.getSeekerRange();
        this.seekerMinTemp = config.getSeekerMinTemp();
        this.turnRate = config.getTurnRate();
        this.navGain = config.getNavGain();
        
        this.armorPenetration = config.getArmorPenetration();
        this.fragCount = config.getFragCount();
        this.fragDamage = config.getFragDamage();
        this.fragRange = config.getFragRange();
        
        switch (warheadType) {
            case HEAT:
                this.explosionPower = Math.max(0.5f, this.explosionPower * 0.3f);
                this.armorPenetration = Math.max(10.0, this.armorPenetration * 2.0);
                break;
            case HEFRAG:
                this.explosionPower = this.explosionPower * 0.7f;
                break;
            case HE:
            default:
                break;
        }
    }

    public void setStoredRotation(float yaw, float pitch) {
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, pitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
    }
    
    public void setPlacementBlockPos(@Nullable BlockPos pos) {
        this.placementBlockPos = pos;
        if (pos != null) {
            this.shipLocalPosition = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }
    
    public void setShipLocalPosition(@Nullable Vec3 pos) {
        this.shipLocalPosition = pos;
    }
    
    public void setShipLocalRotation(float yaw, float pitch) {
        this.shipLocalYaw = yaw;
        this.shipLocalPitch = pitch;
    }
    
    public void setShipLocalRoll(float roll) {
        this.shipLocalRoll = roll;
    }
    
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

    public boolean isAttachedToShip() {
        return this.entityData.get(DATA_ATTACHED_TO_SHIP);
    }
    
    public void setAttachedToShip(boolean attached) {
        this.entityData.set(DATA_ATTACHED_TO_SHIP, attached);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!isDeployed() && isAttachedToShip()) {
            if (shipLocalPosition != null && placementBlockPos != null) {
                Vec3 shipVelocity = VSCompat.getShipVelocity(this.level(), placementBlockPos);
                
                VSCompat.updateEntityPositionOnShip(this, placementBlockPos, shipLocalPosition);
                
                this.setDeltaMovement(shipVelocity);
                
                float worldYaw = VSCompat.transformYawToWorld(this.level(), placementBlockPos, shipLocalYaw);
                float worldPitch = VSCompat.transformPitchToWorld(this.level(), placementBlockPos, shipLocalYaw, shipLocalPitch);
                float worldRoll = VSCompat.transformRollToWorld(this.level(), placementBlockPos, shipLocalRoll);
                
                this.entityData.set(DATA_YAW, worldYaw);
                this.entityData.set(DATA_PITCH, worldPitch);
                this.entityData.set(DATA_ROLL, worldRoll);
                this.setYRot(worldYaw);
                this.setXRot(worldPitch);
                
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
            }
        } else if (!isDeployed()) {
            this.setYRot(getStoredYaw());
            this.setXRot(getStoredPitch());
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        
        if (!this.level().isClientSide) {
            boolean powered = VSCompat.hasRedstoneSignal(this.level(), this.placementBlockPos, this.position());
            if (powered && !lastPowered && !isDeployed()) {
                launch();
            }
            lastPowered = powered;
        }
        
        if (isDeployed()) {
            ticksSinceLaunch++;
            
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                if (MissileConfig.ENABLE_CHUNK_LOADING.get()) {
                    ChunkPos currentChunk = new ChunkPos(this.blockPosition());
                    if (lastForcedChunk == null || !lastForcedChunk.equals(currentChunk)) {
                        if (lastForcedChunk != null) {
                            ForgeChunkManager.forceChunk(serverLevel, "cta", this.blockPosition(), lastForcedChunk.x, lastForcedChunk.z, false, true);
                        }
                        ForgeChunkManager.forceChunk(serverLevel, "cta", this.blockPosition(), currentChunk.x, currentChunk.z, true, true);
                        lastForcedChunk = currentChunk;
                    }
                }
                
                int maxTicks = MissileConfig.MAX_FLIGHT_TICKS.get();
                if (maxTicks > 0 && ticksSinceLaunch > maxTicks) {
                    detonate(this.position());
                    return;
                }
            }
            
            if (this.level().isClientSide && CTADebugCommand.isDebugEnabled() && ticksSinceLaunch % 2 == 0) {
                DebugColor color = switch (warheadType) {
                    case HEAT -> DebugColor.ORANGE;
                    case HEFRAG -> DebugColor.YELLOW;
                    default -> DebugColor.RED;
                };
                MissileDebugRenderer.addFlightPoint(this.getId(), this.position(), color);
            }
            
            applyMissilePhysics();
            
            if (!this.level().isClientSide && guidanceType != GuidanceType.NONE) {
                applyGuidance();
            }
            
            Vec3 motion = this.getDeltaMovement();
            if (motion.lengthSqr() > 0.0001) {
                double hDist = motion.horizontalDistance();
                float targetYaw = (float) (Math.atan2(-motion.x, motion.z) * (180.0 / Math.PI));
                float targetPitch = (float) (Math.atan2(-motion.y, hDist) * (180.0 / Math.PI));
                
                float yawDiff = targetYaw - this.getYRot();
                while (yawDiff > 180) yawDiff -= 360;
                while (yawDiff < -180) yawDiff += 360;
                
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
                this.setYRot(this.getYRot() + yawDiff * 0.3f);
                this.setXRot(this.getXRot() + (targetPitch - this.getXRot()) * 0.3f);
            }
            
            if (!hasImpacted) {
                this.move(MoverType.SELF, this.getDeltaMovement());
            }
            
            spawnFlightParticles();
            
            if (!this.level().isClientSide && ticksSinceLaunch > NOCLIP_TICKS) {
                if (hasImpacted && impactDelayTicks > 0) {
                    impactDelayTicks--;
                    if (impactDelayTicks <= 0) {
                        detonate(this.position());
                        return;
                    }
                }
                
                if (handleFuseLogic()) {
                    return;
                }
                
                if (!hasImpacted && (this.horizontalCollision || this.verticalCollision || this.onGround())) {
                    handleImpact();
                }
            }
        }
    }
    
    protected boolean handleFuseLogic() {
        if (!hasFuze()) return false;
        
        int fuzeType = CBCCompat.getFuzeType(this.fuze);
        
        switch (fuzeType) {
            case CBCCompat.FUZE_TIMED:
                int duration = CBCCompat.getTimedFuzeDuration(this.fuze);
                if (this.ticksSinceLaunch >= duration) {
                    detonate(this.position());
                    return true;
                }
                break;
                
            case CBCCompat.FUZE_PROXIMITY:
                double range = CBCCompat.getProximityRange(this.fuze);
                if (isNearSolidBlock(range)) {
                    detonate(this.position());
                    return true;
                }
                break;
                
            case CBCCompat.FUZE_DELAY:
            case CBCCompat.FUZE_IMPACT:
            default:
                break;
        }
        
        return false;
    }
    
    protected boolean isNearSolidBlock(double range) {

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Vec3 pos = this.position();
        
        for (double dx = -range; dx <= range; dx += 1.0) {
            for (double dy = -range; dy <= range; dy += 1.0) {
                for (double dz = -range; dz <= range; dz += 1.0) {
                    if (dx * dx + dy * dy + dz * dz > range * range) continue;
                    
                    mutable.set(pos.x + dx, pos.y + dy, pos.z + dz);
                    BlockState state = this.level().getBlockState(mutable);
                    if (!state.isAir() && state.isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    protected void handleImpact() {
        if (!hasFuze()) {
            detonate(this.position());
            return;
        }
        
        int fuzeType = CBCCompat.getFuzeType(this.fuze);
        
        switch (fuzeType) {
            case CBCCompat.FUZE_DELAY:
                int delay = CBCCompat.getDelayDuration(this.fuze);
                this.impactDelayTicks = delay;
                this.hasImpacted = true;
                break;
                
            case CBCCompat.FUZE_TIMED:
                this.setDeltaMovement(Vec3.ZERO);
                this.hasImpacted = true;
                break;
                
            case CBCCompat.FUZE_PROXIMITY:
            case CBCCompat.FUZE_IMPACT:
            default:
                detonate(this.position());
                break;
        }
    }
    
    protected void applyMissilePhysics() {
        Vec3 currentMotion = this.getDeltaMovement();
        
        Vec3 thrustDirection;
        if (ticksSinceLaunch <= 3 || currentMotion.lengthSqr() < 0.01) {
            thrustDirection = getForwardVector();
        } else {
            thrustDirection = currentMotion.normalize();
        }
        
        switch (category) {
            case ROCKET:
                if (fuel > 0) {
                    currentMotion = currentMotion.add(thrustDirection.scale(thrust));
                    currentMotion = currentMotion.add(0, -dropRate, 0);
                    fuel--;
                } else {
                    currentMotion = currentMotion.add(0, -gravity, 0);
                }
                currentMotion = currentMotion.scale(airResistance);
                break;
                
            case CRUISE_MISSILE:
                if (fuel > 0) {
                    currentMotion = currentMotion.add(thrustDirection.scale(thrust));
                    fuel--;
                } else {
                    double effectiveGravity = gravity / (1.0 + glideRatio);
                    currentMotion = currentMotion.add(0, -effectiveGravity, 0);
                    if (glideRatio > 0 && currentMotion.horizontalDistance() > 0.01) {
                        Vec3 horizontal = new Vec3(currentMotion.x, 0, currentMotion.z).normalize();
                        currentMotion = currentMotion.add(horizontal.scale(glideRatio * 0.01));
                    }
                }
                currentMotion = currentMotion.scale(airResistance);
                break;
                
            case GUIDED_BOMB:
                double effectiveGravityGlide = gravity / (1.0 + glideRatio * 0.5);
                currentMotion = currentMotion.add(0, -effectiveGravityGlide, 0);
                if (glideRatio > 0 && currentMotion.horizontalDistance() > 0.01) {
                    Vec3 horizontal = new Vec3(currentMotion.x, 0, currentMotion.z).normalize();
                    currentMotion = currentMotion.add(horizontal.scale(glideRatio * 0.005));
                }
                currentMotion = currentMotion.scale(airResistance);
                break;
                
            case RETARDED_BOMB:
                currentMotion = currentMotion.add(0, -gravity, 0);
                double retardedDrag = hasAirbrake ? 0.90 : airResistance;
                currentMotion = new Vec3(
                    currentMotion.x * retardedDrag,
                    currentMotion.y * (hasAirbrake ? 0.92 : 0.995),
                    currentMotion.z * retardedDrag
                );
                break;
                
            case CLUSTER_BOMB:
                currentMotion = currentMotion.add(0, -gravity, 0);
                currentMotion = new Vec3(
                    currentMotion.x * airResistance,
                    currentMotion.y * 0.995,
                    currentMotion.z * airResistance
                );
                break;
                
            case BOMB:
                currentMotion = currentMotion.add(0, -gravity, 0);
                currentMotion = new Vec3(
                    currentMotion.x * airResistance,
                    currentMotion.y * 0.995,
                    currentMotion.z * airResistance
                );
                break;
                
            case MISSILE:
            default:
                if (fuel > 0) {
                    currentMotion = currentMotion.add(thrustDirection.scale(thrust));
                    fuel--;
                } else {
                    currentMotion = currentMotion.add(0, -gravity, 0);
                }
                currentMotion = currentMotion.scale(airResistance);
                break;
        }
        
        double maxSpeed = MissileConfig.MAX_MISSILE_SPEED.get();
        double currentSpeed = currentMotion.length();
        if (currentSpeed > maxSpeed) {
            currentMotion = currentMotion.normalize().scale(maxSpeed);
        }
        
        this.setDeltaMovement(currentMotion);
    }
    
    protected void applyGuidance() {
        if (guidanceType == GuidanceType.NONE) return;
        
        if (seekerCooldown > 0) {
            seekerCooldown--;
        }
        
        switch (guidanceType) {
            case HEAT_SEEKING:
                applyHeatSeekingGuidance();
                break;
            default:
                break;
        }
    }
    
    protected void applyHeatSeekingGuidance() {
        Vec3 missilePos = this.position();
        Vec3 velocity = this.getDeltaMovement();
        double missileSpeed = velocity.length();

        if (missileSpeed < 0.1) return;

        Vec3 lookDirection = velocity.scale(1.0 / missileSpeed);

        if (fuel <= 0 && missileSpeed < 0.8 && ticksSinceLaunch > 40) {
            detonate(this.position());
            return;
        }

        if (currentTargetBlockPos != null && currentTargetPos != null) {
            if (isCurrentTargetOnShip && VSCompat.isVSLoaded()) {
                Vec3 newPos = VSCompat.toWorldCoordinatesRobust(this.level(), currentTargetBlockPos);

                if (previousTargetPos != null && smoothedTargetVel != null) {
                    Vec3 predicted = currentTargetPos.add(smoothedTargetVel);
                    double jumpDistSq = newPos.distanceToSqr(predicted);
                    if (jumpDistSq > 9.0) {
                        currentTargetPos = predicted;
                    } else {
                        currentTargetPos = newPos;
                    }
                } else {
                    currentTargetPos = newPos;
                }
            }
        }

        if (currentTargetPos == null || targetLostTicks > 15) {
            if (seekerCooldown <= 0) {
                searchForTarget(missilePos, lookDirection, false);
            }
        } else if (this.tickCount % 5 == 0 && currentTargetBlockPos != null) {
            try {
                double distSq = missilePos.distanceToSqr(currentTargetPos);
                if (distSq > seekerRange * seekerRange) {
                    targetLostTicks += 3;
                } else if (!isCurrentTargetOnShip
                        && !HeatTargeting.hasLineOfSight(this.level(), missilePos, currentTargetPos)) {
                    targetLostTicks++;
                } else {
                    targetLostTicks = 0;
                }
            } catch (Exception e) {
                targetLostTicks++;
            }
        }

        if (currentTargetPos != null && targetLostTicks <= 15) {
            double rangeToTarget = missilePos.distanceTo(currentTargetPos);

            if (rangeToTarget < 3.0 && ticksSinceLaunch > 20) {
                detonate(this.position());
                return;
            }

            Vec3 toTarget = currentTargetPos.subtract(missilePos);
            if (toTarget.lengthSqr() > 1.0) {
                double cosAngle = lookDirection.dot(toTarget.normalize());
                cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle));
                double trackingAngle = Math.toDegrees(Math.acos(cosAngle));
                double trackingFovLimit = seekerFov * 1.5;

                if (trackingAngle > trackingFovLimit) {
                    resetGuidanceState();
                    noGuidanceTicks++;
                    return;
                }
            }

            noGuidanceTicks = 0;
            steerTowardTarget(currentTargetPos);
        } else {
            noGuidanceTicks++;
        }

        if (targetLostTicks > 20) {
            resetGuidanceState();
        }

        if (noGuidanceTicks > 60 && fuel <= 0) {
            detonate(this.position());
        }
    }

    private void resetGuidanceState() {
        currentTargetPos = null;
        currentTargetBlockPos = null;
        isCurrentTargetOnShip = false;
        targetLostTicks = 0;
        previousTargetPos = null;
        smoothedTargetVel = null;
        previousSmoothedVel = null;
        smoothedTargetAccel = null;
        trackingTicks = 0;
        seekerCooldown = 20;
    }
    
    private void searchForTarget(Vec3 missilePos, Vec3 lookDirection, boolean debug) {
        try {
            HeatAPI heatApi = HeatAPI.get();
            Map<BlockPos, Double> activeSources = heatApi.getActiveHeatSources(this.level(), seekerMinTemp);
            
            if (debug) {
                System.out.println("[HeatSeeker] Missile at " + missilePos + ", tickCount=" + this.tickCount);
                System.out.println("[HeatSeeker] VS loaded=" + VSCompat.isVSLoaded() + ", sources=" + activeSources.size() + " (minTemp=" + seekerMinTemp + "C)");
            }
            
            java.util.List<HeatSourceCandidate> candidates = new java.util.ArrayList<>();
            
            for (Map.Entry<BlockPos, Double> entry : activeSources.entrySet()) {
                BlockPos pos = entry.getKey();
                double temp = entry.getValue();
                
                Vec3 worldPos = Vec3.atCenterOf(pos);
                boolean isShipSource = false;
                
                if (VSCompat.isBlockInShipyard(this.level(), pos)) {
                    isShipSource = true;
                    Vec3 transformed = VSCompat.toWorldCoordinatesRobust(this.level(), pos);
                    
                    if (debug) {
                        boolean sameAsOriginal = (transformed.distanceToSqr(worldPos) < 0.01);
                        System.out.println("[HeatSeeker]   Ship source at " + pos + " (" + temp + "C)");
                        System.out.println("[HeatSeeker]     shipLocal=" + worldPos + " -> worldPos=" + transformed + " (transformOK=" + !sameAsOriginal + ")");
                    }
                    
                    worldPos = transformed;
                } else if (debug && activeSources.size() <= 10) {
                    System.out.println("[HeatSeeker]   World source at " + pos + " (" + temp + "C)");
                }
                
                candidates.add(new HeatSourceCandidate(worldPos, temp, isShipSource, pos));
            }
            
            if (debug) {
                int shipCount = 0;
                for (Object ship : VSCompat.getAllLoadedShips(this.level())) {
                    shipCount++;
                }
                System.out.println("[HeatSeeker] Candidates=" + candidates.size() + ", loadedShips=" + shipCount);
            }
            
            // Find best target
            Vec3 bestWorldPos = null;
            BlockPos bestBlockPos = null;
            boolean bestIsShip = false;
            double bestTemp = seekerMinTemp;
            double rangeSq = seekerRange * seekerRange;
            
            for (HeatSourceCandidate candidate : candidates) {
                Vec3 targetCenter = candidate.worldPos();
                double temp = candidate.temperature();
                
                double distSq = missilePos.distanceToSqr(targetCenter);
                double dist = Math.sqrt(distSq);
                
                if (distSq > rangeSq) {
                    if (debug) System.out.println("[HeatSeeker]   REJECT " + targetCenter + ": range (dist=" + String.format("%.1f", dist) + " > " + seekerRange + ")");
                    continue;
                }
                
                if (temp <= bestTemp && bestWorldPos != null) {
                    if (debug) System.out.println("[HeatSeeker]   REJECT " + targetCenter + ": temp (" + temp + " <= " + bestTemp + ")");
                    continue;
                }
                
                Vec3 toTarget = targetCenter.subtract(missilePos).normalize();
                double dot = lookDirection.dot(toTarget);
                if (dot < 0) {
                    if (debug) System.out.println("[HeatSeeker]   REJECT " + targetCenter + ": behind missile");
                    continue;
                }
                double angle = Math.toDegrees(Math.acos(Math.min(1.0, dot)));
                if (angle > seekerFov) {
                    if (debug) System.out.println("[HeatSeeker]   REJECT " + targetCenter + ": FOV (angle=" + String.format("%.1f", angle) + " > " + seekerFov + ")");
                    continue;
                }

                boolean hasLOS = candidate.isShipSource() || HeatTargeting.hasLineOfSight(this.level(), missilePos, targetCenter);
                if (hasLOS) {
                    if (debug) System.out.println("[HeatSeeker]   LOCK " + targetCenter + ": temp=" + temp + "C, dist=" + String.format("%.1f", dist) + ", angle=" + String.format("%.1f", angle) + ", shipSource=" + candidate.isShipSource());
                    bestWorldPos = targetCenter;
                    bestBlockPos = candidate.blockPos();
                    bestIsShip = candidate.isShipSource();
                    bestTemp = temp;
                } else {
                    if (debug) System.out.println("[HeatSeeker]   REJECT " + targetCenter + ": no LOS");
                }
            }
            
            if (bestWorldPos != null) {
                currentTargetPos = bestWorldPos;
                currentTargetBlockPos = bestBlockPos;
                isCurrentTargetOnShip = bestIsShip;
                targetLostTicks = 0;
                seekerCooldown = 5;
                previousTargetPos = null;
                smoothedTargetVel = null;
                previousSmoothedVel = null;
                smoothedTargetAccel = null;
            } else {
                targetLostTicks++;
                if (debug) System.out.println("[HeatSeeker] No valid target found");
            }
        } catch (Exception e) {
            targetLostTicks++;
        }
    }
    
    protected void steerTowardTarget(Vec3 targetPos) {
        Vec3 missilePos = this.position();
        Vec3 velocity = this.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.01) return;

        Vec3 currentDir = velocity.normalize();

        Vec3 targetVel = Vec3.ZERO;
        if (previousTargetPos != null) {
            Vec3 rawVel = targetPos.subtract(previousTargetPos);
            if (rawVel.lengthSqr() < 25.0) {
                if (smoothedTargetVel == null) {
                    smoothedTargetVel = rawVel;
                } else {
                    smoothedTargetVel = smoothedTargetVel.scale(0.3).add(rawVel.scale(0.7));
                }
                trackingTicks++;
            }
        }
        if (smoothedTargetVel != null) {
            targetVel = smoothedTargetVel;
        }

        Vec3 targetAccel = Vec3.ZERO;
        if (trackingTicks >= 5 && previousSmoothedVel != null && smoothedTargetVel != null) {
            Vec3 rawAccel = smoothedTargetVel.subtract(previousSmoothedVel);
            if (rawAccel.lengthSqr() < 4.0) {
                if (smoothedTargetAccel == null) {
                    smoothedTargetAccel = rawAccel;
                } else {
                    smoothedTargetAccel = smoothedTargetAccel.scale(0.3).add(rawAccel.scale(0.7));
                }
            }
        }
        if (smoothedTargetAccel != null) {
            targetAccel = smoothedTargetAccel;
        }

        previousSmoothedVel = smoothedTargetVel;
        previousTargetPos = targetPos;

        Vec3 desiredDir;
        switch (steeringMethod) {
            case PROPORTIONAL_NAV:
                desiredDir = computeProportionalNav(missilePos, velocity, targetPos, targetVel, targetAccel, speed);
                break;
            case LEAD_PURSUIT:
                desiredDir = computeLeadPursuit(missilePos, velocity, targetPos, targetVel, targetAccel, speed);
                break;
            case COMMAND_LOS:
                desiredDir = computeCommandLOS(missilePos, velocity, targetPos);
                break;
            case PURE_PURSUIT:
            default:
                desiredDir = targetPos.subtract(missilePos).normalize();
                break;
        }

        if (desiredDir == null || desiredDir.lengthSqr() < 0.001 || Double.isNaN(desiredDir.x)) {
            Vec3 fallback = targetPos.subtract(missilePos);
            if (fallback.lengthSqr() < 0.001) return;
            desiredDir = fallback.normalize();
        } else {
            desiredDir = desiredDir.normalize();
        }

        applyTurnRateLimit(currentDir, desiredDir, speed);
    }
    
    private Vec3 computeProportionalNav(Vec3 missilePos, Vec3 velocity, Vec3 targetPos,
                                         Vec3 targetVel, Vec3 targetAccel, double speed) {
        Vec3 relPos = targetPos.subtract(missilePos);
        double range = relPos.length();
        if (range < 0.5) return relPos.normalize();

        Vec3 relVel = targetVel.subtract(velocity);
        Vec3 Rhat = relPos.scale(1.0 / range);

        double tgo = Math.max(estimateInterceptTime(missilePos, targetPos, targetVel, targetAccel, speed), 1.0);

        Vec3 zem = relPos.add(relVel.scale(tgo));

        if (targetAccel.lengthSqr() > 0.000001) {
            zem = zem.add(targetAccel.scale(0.5 * tgo * tgo));
        }

        double tgoSq = tgo * tgo;
        Vec3 accel = zem.scale(navGain / tgoSq);

        Vec3 desiredVel = velocity.add(accel);
        return desiredVel.lengthSqr() > 0.001 ? desiredVel.normalize() : Rhat;
    }
    

    private Vec3 computeLeadPursuit(Vec3 missilePos, Vec3 velocity, Vec3 targetPos,
                                     Vec3 targetVel, Vec3 targetAccel, double speed) {
        Vec3 toTarget = targetPos.subtract(missilePos);
        double distance = toTarget.length();
        if (distance < 1.0) return toTarget.normalize();
        if (speed < 0.1) return toTarget.normalize();

        double targetSpeed = targetVel.length();
        if (targetSpeed < 0.001) return toTarget.normalize();

        double tIntercept = estimateInterceptTime(missilePos, targetPos, targetVel, targetAccel, speed);
        Vec3 predictedPos = predictTargetPosition(targetPos, targetVel, targetAccel, tIntercept);

        Vec3 toPredicted = predictedPos.subtract(missilePos);
        return toPredicted.lengthSqr() > 0.001 ? toPredicted.normalize() : toTarget.normalize();
    }

    private double estimateInterceptTime(Vec3 missilePos, Vec3 targetPos, Vec3 targetVel,
                                         Vec3 targetAccel, double missileSpeed) {
        double clampedMissileSpeed = Math.max(missileSpeed, 0.1);
        Vec3 relPos = targetPos.subtract(missilePos);
        double distance = relPos.length();
        if (distance < 0.5) return 0.0;

        double maxPredictionTicks = Math.min(Math.max(fuel > 0 ? fuel * 1.25 : 60.0, 15.0), 200.0);
        double tIntercept = solveLinearInterceptTime(relPos, targetVel, clampedMissileSpeed);
        if (!Double.isFinite(tIntercept) || tIntercept <= 0.0) {
            tIntercept = distance / clampedMissileSpeed;
        }

        tIntercept = Math.max(0.0, Math.min(tIntercept, maxPredictionTicks));

        for (int i = 0; i < 3; i++) {
            Vec3 predictedPos = predictTargetPosition(targetPos, targetVel, targetAccel, tIntercept);
            double nextIntercept = predictedPos.subtract(missilePos).length() / clampedMissileSpeed;
            nextIntercept = Math.max(0.0, Math.min(nextIntercept, maxPredictionTicks));
            if (Math.abs(nextIntercept - tIntercept) < 0.25) {
                break;
            }
            tIntercept = nextIntercept;
        }

        return tIntercept;
    }

    private double solveLinearInterceptTime(Vec3 relPos, Vec3 targetVel, double missileSpeed) {
        double a = targetVel.lengthSqr() - missileSpeed * missileSpeed;
        double b = 2.0 * relPos.dot(targetVel);
        double c = relPos.lengthSqr();

        if (Math.abs(a) < 1.0e-6) {
            if (Math.abs(b) < 1.0e-6) {
                return Double.NaN;
            }
            double linearRoot = -c / b;
            return linearRoot > 0.0 ? linearRoot : Double.NaN;
        }

        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) {
            return Double.NaN;
        }

        double sqrtDiscriminant = Math.sqrt(discriminant);
        double t1 = (-b - sqrtDiscriminant) / (2.0 * a);
        double t2 = (-b + sqrtDiscriminant) / (2.0 * a);
        double bestRoot = Double.NaN;

        if (t1 > 0.0) {
            bestRoot = t1;
        }
        if (t2 > 0.0 && (!Double.isFinite(bestRoot) || t2 < bestRoot)) {
            bestRoot = t2;
        }

        return bestRoot;
    }

    private Vec3 predictTargetPosition(Vec3 targetPos, Vec3 targetVel, Vec3 targetAccel, double timeAhead) {
        if (timeAhead <= 0.0) {
            return targetPos;
        }

        double targetSpeed = targetVel.length();
        if (targetAccel.lengthSqr() > 0.000001 && targetSpeed > 0.01) {
            Vec3 omegaVec = crossProduct(targetVel, targetAccel)
                    .scale(1.0 / (targetSpeed * targetSpeed));
            double omegaMag = omegaVec.length();

            if (omegaMag > 0.00001) {
                double angle = Math.min(omegaMag * timeAhead, Math.PI);
                Vec3 omegaAxis = omegaVec.scale(1.0 / omegaMag);
                Vec3 midVel = rotateAroundAxis(targetVel, omegaAxis, angle * 0.5);
                double chordScale = (angle > 0.01)
                        ? 2.0 * Math.sin(angle * 0.5) / omegaMag
                        : timeAhead;
                return targetPos.add(midVel.scale(chordScale));
            }
        }

        return targetPos.add(targetVel.scale(timeAhead));
    }
    
    private Vec3 computeCommandLOS(Vec3 missilePos, Vec3 velocity, Vec3 targetPos) {
        Vec3 origin = launchWorldPos != null ? launchWorldPos : missilePos;

        Vec3 losLine = targetPos.subtract(origin);
        double losLength = losLine.length();
        if (losLength < 1.0) return targetPos.subtract(missilePos).normalize();

        Vec3 losUnit = losLine.normalize();

        Vec3 missileOffset = missilePos.subtract(origin);
        double projLength = missileOffset.dot(losUnit);
        Vec3 closestOnLine = origin.add(losUnit.scale(projLength));

        Vec3 lateralError = closestOnLine.subtract(missilePos);
        double errorMag = lateralError.length();

        Vec3 toTarget = targetPos.subtract(missilePos);
        if (toTarget.lengthSqr() < 0.01) return losUnit;
        Vec3 toTargetDir = toTarget.normalize();

        if (errorMag < 0.01) {
            return toTargetDir;
        }

        double correctionWeight = Math.min(1.0, errorMag * 2.0);
        Vec3 correctionDir = lateralError.normalize();
        Vec3 blended = toTargetDir.scale(1.0 - correctionWeight * 0.5)
                .add(correctionDir.scale(correctionWeight * 0.5));
        return blended.lengthSqr() > 0.001 ? blended.normalize() : toTargetDir;
    }
    
    private void applyTurnRateLimit(Vec3 currentDir, Vec3 desiredDir, double speed) {
        double dot = currentDir.dot(desiredDir);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angleDeg = Math.toDegrees(Math.acos(dot));

        if (angleDeg < 0.3) return;

        double speedFactor = Math.min(1.0, speed / 1.0);
        double effectiveTurnRate = turnRate * speedFactor;

        double t = Math.min(1.0, effectiveTurnRate / angleDeg);
        Vec3 newDir = slerp(currentDir, desiredDir, t);
        this.setDeltaMovement(newDir.scale(speed));
    }
    
    protected Vec3 slerp(Vec3 from, Vec3 to, double t) {
        double dot = from.dot(to);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        
        double theta = Math.acos(dot);
        if (Math.abs(theta) < 0.001) {
            return from.scale(1 - t).add(to.scale(t)).normalize();
        }
        
        double sinTheta = Math.sin(theta);
        double a = Math.sin((1 - t) * theta) / sinTheta;
        double b = Math.sin(t * theta) / sinTheta;
        
        return from.scale(a).add(to.scale(b)).normalize();
    }
    
    private static Vec3 crossProduct(Vec3 a, Vec3 b) {
        return new Vec3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        );
    }
    
    protected Vec3 getForwardVector() {
        float yawRad = (float) Math.toRadians(this.getYRot());
        float pitchRad = (float) Math.toRadians(this.getXRot());
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3(x, y, z);
    }
    
    protected void spawnFlightParticles() {
        if (!this.level().isClientSide) return;
        
        Vec3 pos = this.position();
        
        if (fuel > 0) {
            switch (category) {
                case ROCKET:
                    this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            pos.x, pos.y, pos.z, 0, 0, 0);
                    this.level().addParticle(ParticleTypes.FLAME,
                            pos.x, pos.y, pos.z, 
                            (random.nextDouble() - 0.5) * 0.1,
                            (random.nextDouble() - 0.5) * 0.1,
                            (random.nextDouble() - 0.5) * 0.1);
                    break;
                case CRUISE_MISSILE:
                    this.level().addParticle(ParticleTypes.SMOKE,
                            pos.x, pos.y, pos.z, 0, 0, 0);
                    break;
                default:
                    this.level().addParticle(ParticleTypes.SMOKE,
                            pos.x, pos.y, pos.z, 0, 0, 0);
                    if (ticksSinceLaunch % 2 == 0) {
                        this.level().addParticle(ParticleTypes.FLAME,
                                pos.x, pos.y, pos.z, 0, 0, 0);
                    }
                    break;
            }
        } else if (hasAirbrake && category == MissileCategory.RETARDED_BOMB) {
            if (ticksSinceLaunch % 5 == 0) {
                this.level().addParticle(ParticleTypes.CLOUD,
                        pos.x + (random.nextDouble() - 0.5) * 0.5,
                        pos.y + (random.nextDouble() - 0.5) * 0.5,
                        pos.z + (random.nextDouble() - 0.5) * 0.5,
                        0, 0, 0);
            }
        }
    }

    public boolean launch() {
        if (isDeployed()) return false;
        
        BlockPos referencePos = placementBlockPos != null ? placementBlockPos : this.blockPosition();
        Vec3 launchPos = this.position();
        Vec3 shipVelocity = Vec3.ZERO;
        
        if (isAttachedToShip() && shipLocalPosition != null && placementBlockPos != null) {
            launchPos = VSCompat.toWorldCoordinates(this.level(), placementBlockPos, shipLocalPosition);
            
            shipVelocity = VSCompat.getShipVelocityAtPoint(this.level(), referencePos, launchPos);
            
            float worldYaw = VSCompat.transformYawToWorld(this.level(), placementBlockPos, shipLocalYaw);
            float worldPitch = VSCompat.transformPitchToWorld(this.level(), placementBlockPos, shipLocalYaw, shipLocalPitch);
            
            this.setPos(launchPos.x, launchPos.y, launchPos.z);
            this.setYRot(worldYaw);
            this.setXRot(worldPitch);
            this.yRotO = worldYaw;
            this.xRotO = worldPitch;
            
            this.entityData.set(DATA_YAW, worldYaw);
            this.entityData.set(DATA_PITCH, worldPitch);
        }
        
        this.setAttachedToShip(false);
        this.shipLocalPosition = null;
        this.entityData.set(DATA_DEPLOYED, true);
        this.ticksSinceLaunch = 0;
        
        this.launchWorldPos = launchPos;
        
        Vec3 forward = getForwardVector();
        
        if (isBomb) {
            Vec3 horizontalShipVel = new Vec3(shipVelocity.x, 0, shipVelocity.z);
            this.setDeltaMovement(horizontalShipVel);
        } else {
            Vec3 initialVel = forward.scale(initialSpeed);
            this.setDeltaMovement(initialVel.add(shipVelocity));
        }
        
        return true;
    }

    protected void detonate(Vec3 pos) {
        if (!this.level().isClientSide && !hasDetonated) {
            hasDetonated = true;
            releaseChunkLoad();
            
            switch (warheadType) {
                case HEAT:
                    performHeatDetonation(pos);
                    break;
                case HEFRAG:
                    performHefragDetonation(pos);
                    break;
                case HE:
                default:
                    performHeDetonation(pos);
                    break;
            }
        }
    }
    
    protected void performHeDetonation(Vec3 pos) {
        MissileDebugRenderer.addExplosion(pos, explosionPower, DebugColor.RED);
        
        this.discard();
        this.level().explode(null, pos.x, pos.y, pos.z, explosionPower, Level.ExplosionInteraction.TNT);
    }
    
    protected void performHeatDetonation(Vec3 pos) {
        Vec3 penetrationDirection;
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 0.0001) {
            penetrationDirection = motion.normalize();
        } else {
            float yawRad = (float) Math.toRadians(this.getYRot());
            float pitchRad = (float) Math.toRadians(this.getXRot());
            penetrationDirection = new Vec3(
                    -Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad)
            );
        }
        
        double penetrationPower = MissileConfig.HEAT_BASE_PENETRATION.get();
        double remainingPenetration = penetrationPower;
        
        Level level = this.level();
        Vec3 currentPos = pos;
        Vec3 jetEndPos = pos;
        boolean passedThroughArmor = false;
        boolean reachedAir = false;
        int blocksDestroyed = 0;
        int maxIterations = 50;
        
        for (int i = 0; i < maxIterations && remainingPenetration > 0; i++) {
            BlockPos blockPos = BlockPos.containing(currentPos);
            
            if (!penetratedBlocks.contains(blockPos)) {
                BlockState state = level.getBlockState(blockPos);
                
                if (!state.isAir()) {
                    double blockToughness;
                    if (MissileConfig.USE_CBC_BLOCK_RESISTANCE.get()) {
                        blockToughness = CBCIntegration.getBlockToughness(level, state, blockPos);
                    } else {
                        float hardness = state.getDestroySpeed(level, blockPos);
                        if (hardness < 0) {
                            blockToughness = Double.MAX_VALUE;
                        } else {
                            blockToughness = state.getBlock().getExplosionResistance() * 2;
                        }
                    }
                    
                    if (remainingPenetration < blockToughness) {
                        jetEndPos = currentPos;
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.LAVA,
                                    currentPos.x, currentPos.y, currentPos.z,
                                    10, 0.2, 0.2, 0.2, 0.1);
                        }
                        this.discard();
                        return;
                    }
                    
                    passedThroughArmor = true;
                    penetratedBlocks.add(blockPos);
                    
                    remainingPenetration -= blockToughness;
                    blocksDestroyed++;
                    
                    if (level instanceof ServerLevel serverLevel) {
                        level.destroyBlock(blockPos, false);
                        
                        serverLevel.sendParticles(ParticleTypes.LAVA,
                                currentPos.x, currentPos.y, currentPos.z,
                                5, 0.1, 0.1, 0.1, 0.05);
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                currentPos.x, currentPos.y, currentPos.z,
                                3, 0.05, 0.05, 0.05, 0.02);
                    }
                } else {
                    if (passedThroughArmor) {
                        jetEndPos = currentPos;
                        reachedAir = true;
                        break;
                    }
                }
            }
            
            currentPos = currentPos.add(penetrationDirection.scale(0.25));
            jetEndPos = currentPos;
        }
        
        boolean succeeded = passedThroughArmor && reachedAir;
        MissileDebugRenderer.addPenetrationPath(pos, jetEndPos, succeeded);
        
        this.discard();
        
        if (passedThroughArmor && reachedAir) {
            float behindArmorPower = Math.max(2.0f, baseExplosionPower * 0.5f);
            MissileDebugRenderer.addExplosion(jetEndPos, behindArmorPower, DebugColor.ORANGE);
            
            this.level().explode(null, jetEndPos.x, jetEndPos.y, jetEndPos.z, 
                    behindArmorPower, Level.ExplosionInteraction.TNT);
            
            if (level instanceof ServerLevel serverLevel) {
                createSpallCone(serverLevel, jetEndPos, penetrationDirection);
            }
        } else if (!passedThroughArmor) {
            MissileDebugRenderer.addExplosion(pos, Math.max(1.0f, baseExplosionPower * 0.2f), DebugColor.ORANGE);
            this.level().explode(null, pos.x, pos.y, pos.z,
                    Math.max(1.0f, baseExplosionPower * 0.2f), Level.ExplosionInteraction.TNT);
        }
    }
    
    private void createSpallCone(ServerLevel level, Vec3 center, Vec3 direction) {
        double spallRange = armorPenetration * 0.5;
        AABB damageArea = new AABB(
                center.x - spallRange, center.y - spallRange, center.z - spallRange,
                center.x + spallRange, center.y + spallRange, center.z + spallRange
        );
        
        List<Entity> entities = level.getEntities(this, damageArea, e -> e instanceof LivingEntity && e.isAlive());
        DamageSource damageSource = level.damageSources().explosion(null, null);
        
        for (Entity entity : entities) {
            Vec3 toEntity = entity.position().subtract(center);
            double distance = toEntity.length();
            
            double dot = direction.dot(toEntity.normalize());
            if (dot > 0.3 && distance < spallRange) {
                float damage = (float) ((1.0 - distance / spallRange) * explosionPower * 4);
                ((LivingEntity) entity).hurt(damageSource, damage);
                
                level.sendParticles(ParticleTypes.CRIT,
                        center.x, center.y, center.z, 5,
                        toEntity.x * 0.1, toEntity.y * 0.1, toEntity.z * 0.1, 0.3);
            }
        }
    }
    
    protected void performHefragDetonation(Vec3 pos) {
        MissileDebugRenderer.addExplosion(pos, explosionPower, DebugColor.YELLOW);
        
        this.level().explode(null, pos.x, pos.y, pos.z, explosionPower, Level.ExplosionInteraction.TNT);
        
        if (this.level() instanceof ServerLevel serverLevel) {
            releaseFragments(serverLevel, pos);
        }
        
        this.discard();
    }
    
    private void releaseFragments(ServerLevel level, Vec3 detonationPos) {
        double multiplier = MissileConfig.FRAGMENT_COUNT_MULTIPLIER.get();
        int actualFragCount = (int) (fragCount * 3 * multiplier);
        float fragmentSpeed = 2.0f;
        
        Vec3 travelDirection = this.getDeltaMovement();
        if (travelDirection.lengthSqr() < 0.001) {
            float yawRad = (float) Math.toRadians(this.getYRot());
            float pitchRad = (float) Math.toRadians(this.getXRot());
            travelDirection = new Vec3(
                    -Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad)
            );
        }
        travelDirection = travelDirection.normalize();
        
        for (int i = 0; i < actualFragCount; i++) {
            Vec3 fragDir;
            
            if (isBomb) {
                double theta = random.nextDouble() * Math.PI * 2;
                double phi = random.nextDouble() * Math.PI;
                
                fragDir = new Vec3(
                        Math.sin(phi) * Math.cos(theta),
                        Math.cos(phi),
                        Math.sin(phi) * Math.sin(theta)
                );
            } else {
                double coneAngle = Math.toRadians(60);
                
                double theta = random.nextDouble() * Math.PI * 2;
                double phi = random.nextDouble() * coneAngle;
                
                Vec3 localDir = new Vec3(
                        Math.sin(phi) * Math.cos(theta),
                        Math.sin(phi) * Math.sin(theta),
                        Math.cos(phi)
                );
                
                fragDir = rotateVectorToDirection(localDir, travelDirection);
            }
            
            double speedVariation = 0.7 + random.nextDouble() * 0.6;
            Vec3 velocity = fragDir.scale(fragmentSpeed * speedVariation);
            
            FragmentEntity fragment = FragmentEntity.create(level, detonationPos, velocity, (float) fragDamage);
            level.addFreshEntity(fragment);
            
            if (i % 5 == 0) {
                MissileDebugRenderer.addFragment(detonationPos, fragDir, fragRange, false);
            }
        }
        
        level.playSound(null, BlockPos.containing(detonationPos), 
                SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0f, 0.5f);
    }
    
    private Vec3 rotateVectorToDirection(Vec3 local, Vec3 targetDir) {
        Vec3 zAxis = new Vec3(0, 0, 1);
        
        if (Math.abs(targetDir.dot(zAxis)) > 0.999) {
            if (targetDir.z > 0) {
                return local;
            } else {
                return new Vec3(local.x, local.y, -local.z);
            }
        }
        
        Vec3 rotationAxis = zAxis.cross(targetDir).normalize();
        double angle = Math.acos(zAxis.dot(targetDir));
        
        return rotateAroundAxis(local, rotationAxis, angle);
    }
    
    private Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        Vec3 term1 = v.scale(cos);
        Vec3 term2 = axis.cross(v).scale(sin);
        Vec3 term3 = axis.scale(axis.dot(v) * (1 - cos));
        
        return term1.add(term2).add(term3);
    }
    
    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from).normalize();
        double distance = from.distanceTo(to);
        
        for (double d = 0.5; d < distance; d += 0.5) {
            Vec3 checkPos = from.add(direction.scale(d));
            BlockPos blockPos = BlockPos.containing(checkPos);
            BlockState state = this.level().getBlockState(blockPos);
            
            if (!state.isAir() && state.blocksMotion()) {
                return false;
            }
        }
        return true;
    }

    private void releaseChunkLoad() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel && lastForcedChunk != null) {
            ForgeChunkManager.forceChunk(serverLevel, "cta", this.blockPosition(), lastForcedChunk.x, lastForcedChunk.z, false, true);
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
            this.fuel = 0;
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (isCreateWrench(heldItem)) {
                float delta = 15;
                if (player.isShiftKeyDown()) {
                    float newPitch = getStoredPitch() + delta;
                    if (newPitch > 90) newPitch = -90 + (newPitch - 90);
                    if (newPitch < -90) newPitch = 90 + (newPitch + 90);
                    setStoredRotation(getStoredYaw(), newPitch);
                    this.shipLocalPitch = newPitch;
                } else {
                    float newYaw = getStoredYaw() + delta;
                    if (newYaw >= 360) newYaw -= 360;
                    if (newYaw < 0) newYaw += 360;
                    setStoredRotation(newYaw, getStoredPitch());
                    this.shipLocalYaw = newYaw;
                }
                return InteractionResult.SUCCESS;
            }
            
            if (CBCCompat.isFuzeItem(heldItem)) {
                if (isDeployed()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Cannot change fuse on deployed missile!").withStyle(net.minecraft.ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
                
                if (hasFuze() && !player.getAbilities().instabuild) {
                    player.getInventory().add(this.fuze.copy());
                }
                
                ItemStack fuzeToApply = player.getAbilities().instabuild ? heldItem.copy() : heldItem.split(1);
                fuzeToApply.setCount(1);
                setFuze(fuzeToApply);
                
                this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.5f);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Fuse installed: " + heldItem.getHoverName().getString()).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                
                return InteractionResult.SUCCESS;
            }
            
            if (player.isShiftKeyDown()) {
                if (!player.getAbilities().instabuild) {
                    player.getInventory().add(modelItem.copy());
                    if (hasFuze()) {
                        player.getInventory().add(this.fuze.copy());
                    }
                }
                this.discard();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
    
    private boolean isCreateWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
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
        
        if (compound.contains("WarheadType")) {
            int warheadOrdinal = compound.getInt("WarheadType");
            this.warheadType = WarheadType.values()[Math.min(warheadOrdinal, WarheadType.values().length - 1)];
            this.entityData.set(DATA_WARHEAD_TYPE, warheadOrdinal);
        }
        
        if (compound.contains("Fuze", Tag.TAG_COMPOUND)) {
            this.fuze = ItemStack.of(compound.getCompound("Fuze"));
        } else {
            this.fuze = ItemStack.EMPTY;
        }
        
        if (compound.contains("MissileId")) {
            this.missileId = compound.getString("MissileId");
            this.entityData.set(DATA_MISSILE_ID, this.missileId);
            loadConfigValues();
        }
        
        if (compound.contains("ModelItem")) {
            this.modelItem = ItemStack.of(compound.getCompound("ModelItem"));
        }
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
        this.shipLocalYaw = compound.getFloat("ShipLocalYaw");
        this.shipLocalPitch = compound.getFloat("ShipLocalPitch");
        this.shipLocalRoll = compound.getFloat("ShipLocalRoll");
        
        if (compound.contains("AttachedToShip")) {
            this.entityData.set(DATA_ATTACHED_TO_SHIP, compound.getBoolean("AttachedToShip"));
        }
        
        this.hasImpacted = compound.getBoolean("HasImpacted");
        this.impactDelayTicks = compound.getInt("ImpactDelayTicks");
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
        compound.putString("MissileId", this.missileId);
        compound.putInt("WarheadType", this.warheadType.ordinal());
        if (!this.fuze.isEmpty()) {
            compound.put("Fuze", this.fuze.save(new CompoundTag()));
        }
        compound.put("ModelItem", this.modelItem.save(new CompoundTag()));
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
        compound.putFloat("ShipLocalYaw", this.shipLocalYaw);
        compound.putFloat("ShipLocalPitch", this.shipLocalPitch);
        compound.putFloat("ShipLocalRoll", this.shipLocalRoll);
        
        compound.putBoolean("AttachedToShip", this.entityData.get(DATA_ATTACHED_TO_SHIP));
        
        compound.putBoolean("HasImpacted", this.hasImpacted);
        compound.putInt("ImpactDelayTicks", this.impactDelayTicks);
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
        buffer.writeUtf(this.missileId);
        buffer.writeInt(this.warheadType.ordinal());
        buffer.writeItem(this.fuze);
        buffer.writeBoolean(this.entityData.get(DATA_ATTACHED_TO_SHIP));
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.modelItem = buffer.readItem();
        this.entityData.set(DATA_DEPLOYED, buffer.readBoolean());
        this.entityData.set(DATA_YAW, buffer.readFloat());
        this.entityData.set(DATA_PITCH, buffer.readFloat());
        this.isBomb = buffer.readBoolean();
        this.missileId = buffer.readUtf();
        int warheadOrdinal = buffer.readInt();
        this.warheadType = WarheadType.values()[Math.min(warheadOrdinal, WarheadType.values().length - 1)];
        this.fuze = buffer.readItem();
        this.entityData.set(DATA_ATTACHED_TO_SHIP, buffer.readBoolean());
        this.entityData.set(DATA_MISSILE_ID, this.missileId);
        this.entityData.set(DATA_WARHEAD_TYPE, warheadOrdinal);
        loadConfigValues();
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
        if (!isDeployed() && isAttachedToShip()) {
            this.setPos(x, y, z);
            this.setYRot(yaw);
            this.setXRot(pitch);
        } else {
            super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
        }
    }
    
    private static record HeatSourceCandidate(Vec3 worldPos, double temperature, boolean isShipSource, BlockPos blockPos) {}
}
