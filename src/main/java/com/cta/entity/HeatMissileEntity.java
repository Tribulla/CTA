package com.cta.entity;

import com.cta.config.MissileConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeatMissileEntity extends MissileEntity {
    
    private int penetrationDepth = 0;
    private final int maxPenetration;
    private Vec3 penetrationDirection = null;
    private final Set<BlockPos> penetratedBlocks = new HashSet<>();
    
    public HeatMissileEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        MissileConfig.MissileTypeConfig config = MissileConfig.getConfig("heat_missile");
        this.maxPenetration = (int) config.getArmorPenetration();
        this.explosionPower = config.getExplosionPower();
        this.fuel = config.getFuelTicks();
        this.missileId = "heat_missile";
        loadConfigValues();
    }
    
    @Override
    protected void detonate(Vec3 pos) {
        if (!this.level().isClientSide && !hasDetonated) {
            if (penetrationDirection == null) {
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
            }
            
            performJetPenetration(pos);
        }
    }
    
    private void performJetPenetration(Vec3 impactPos) {
        Level level = this.level();
        Vec3 currentPos = impactPos;
        Vec3 jetEndPos = impactPos;
        boolean passedThroughArmor = false;
        
        for (int i = 0; i < maxPenetration * 2 && penetrationDepth < maxPenetration; i++) {
            BlockPos blockPos = BlockPos.containing(currentPos);
            
            if (!penetratedBlocks.contains(blockPos)) {
                BlockState state = level.getBlockState(blockPos);
                
                if (!state.isAir()) {
                    float hardness = state.getDestroySpeed(level, blockPos);
                    
                    if (hardness < 0 || hardness > 50) {
                        jetEndPos = currentPos;
                        break;
                    }
                    
                    passedThroughArmor = true;
                    penetratedBlocks.add(blockPos);
                    
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.LAVA,
                                currentPos.x, currentPos.y, currentPos.z,
                                3, 0.05, 0.05, 0.05, 0.02);
                    }
                    
                    penetrationDepth++;
                    if (hardness > 5) {
                        penetrationDepth += (int)(hardness / 5);
                    }
                    
                    if (penetrationDepth >= maxPenetration) {
                        jetEndPos = currentPos;
                        break;
                    }
                } else {
                    if (passedThroughArmor) {
                        jetEndPos = currentPos;
                        break;
                    }
                }
            }
            
            currentPos = currentPos.add(penetrationDirection.scale(0.25));
            jetEndPos = currentPos;
        }
        
        finalDetonation(jetEndPos, passedThroughArmor);
    }

    private void finalDetonation(Vec3 pos, boolean penetratedArmor) {
        if (!hasDetonated) {
            hasDetonated = true;
            releaseChunkLoadInternal();
            this.discard();
            
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, BlockPos.containing(pos), 
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.5f);
                
                if (penetratedArmor) {
                    for (int i = 0; i < 15; i++) {
                        double spread = 0.3;
                        Vec3 spallDir = penetrationDirection.add(
                                (random.nextDouble() - 0.5) * spread,
                                (random.nextDouble() - 0.5) * spread,
                                (random.nextDouble() - 0.5) * spread
                        );
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                pos.x, pos.y, pos.z,
                                1, spallDir.x * 0.5, spallDir.y * 0.5, spallDir.z * 0.5, 0.1);
                    }
                    
                    damageEntitiesInSpallCone(pos, serverLevel);
                    
                    serverLevel.explode(null, pos.x, pos.y, pos.z,
                            explosionPower * 0.5f, Level.ExplosionInteraction.NONE);
                } else {
                    serverLevel.explode(null, pos.x, pos.y, pos.z,
                            explosionPower, Level.ExplosionInteraction.TNT);
                }
            }
        }
    }

    private void damageEntitiesInSpallCone(Vec3 detonationPos, ServerLevel level) {
        double coneLength = 5.0;
        double coneAngle = 30.0;
        
        AABB searchArea = new AABB(
                detonationPos.x - coneLength, detonationPos.y - coneLength, detonationPos.z - coneLength,
                detonationPos.x + coneLength, detonationPos.y + coneLength, detonationPos.z + coneLength
        );
        
        List<Entity> entities = level.getEntities(this, searchArea,
                e -> e instanceof LivingEntity && e.isAlive());
        
        DamageSource spallDamage = level.damageSources().explosion(null, null);
        
        for (Entity entity : entities) {
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(detonationPos);
            double distance = toEntity.length();
            
            if (distance > coneLength || distance < 0.1) continue;
            
            double dotProduct = toEntity.normalize().dot(penetrationDirection);
            double angleToEntity = Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, dotProduct))));
            
            if (angleToEntity <= coneAngle) {
                float damage = (float) (20.0 * (1.0 - distance / coneLength));
                if (entity instanceof LivingEntity living) {
                    living.hurt(spallDamage, damage);
                    
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                            (int)(damage / 2), 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }
    
    private void releaseChunkLoadInternal() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            if (super.lastForcedChunk != null) {
                net.minecraftforge.common.world.ForgeChunkManager.forceChunk(
                        serverLevel, "cta", this.blockPosition(), 
                        super.lastForcedChunk.x, super.lastForcedChunk.z, false, false);
                super.lastForcedChunk = null;
            }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide && isDeployed() && fuel > 0) {
            Vec3 pos = this.position();
            this.level().addParticle(ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1);
        }
    }
}
