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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FragMissileEntity extends MissileEntity {
    
    private final int fragCount;
    private final double fragDamage;
    private final double fragRange;
    
    public FragMissileEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        MissileConfig.MissileTypeConfig config = MissileConfig.getConfig("frag_missile");
        this.fragCount = config.getFragCount();
        this.fragDamage = config.getFragDamage();
        this.fragRange = config.getFragRange();
        this.explosionPower = config.getExplosionPower();
        this.fuel = config.getFuelTicks();
        this.missileId = "frag_missile";
        loadConfigValues();
    }
    
    @Override
    protected void detonate(Vec3 pos) {
        if (!this.level().isClientSide && !hasDetonated) {
            hasDetonated = true;
            releaseChunkLoadInternal();
            
            this.level().explode(null, pos.x, pos.y, pos.z, 
                    explosionPower, Level.ExplosionInteraction.TNT);
            
            releaseFragments(pos);
            
            this.discard();
        }
    }
    
    private void releaseFragments(Vec3 detonationPos) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        AABB damageArea = new AABB(
                detonationPos.x - fragRange, detonationPos.y - fragRange, detonationPos.z - fragRange,
                detonationPos.x + fragRange, detonationPos.y + fragRange, detonationPos.z + fragRange
        );
        
        List<Entity> nearbyEntities = this.level().getEntities(this, damageArea, 
                e -> e instanceof LivingEntity && e.isAlive());
        
        for (int i = 0; i < fragCount; i++) {
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.cos(phi);
            double z = Math.sin(phi) * Math.sin(theta);
            
            Vec3 fragDir = new Vec3(x, y, z);
            
            for (int j = 0; j < 5; j++) {
                double dist = (j + 1) * (fragRange / 5);
                Vec3 particlePos = detonationPos.add(fragDir.scale(dist));
                
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0);
            }
        }
        
        DamageSource fragDamageSource = this.level().damageSources().explosion(null, null);
        
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity living)) continue;
            
            Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);
            double distance = detonationPos.distanceTo(entityPos);
            
            if (distance > fragRange) continue;
            
            if (!hasLineOfSight(detonationPos, entityPos)) {
                continue;
            }
            
            double distanceFactor = 1.0 - (distance / fragRange);
            distanceFactor = distanceFactor * distanceFactor;
            
            int fragmentsHit = (int) (fragCount * distanceFactor * 0.3 * 
                    (entity.getBbWidth() * entity.getBbHeight() / 4.0));
            fragmentsHit = Math.max(1, Math.min(fragmentsHit, fragCount / 2));
            
            float totalDamage = (float) (fragmentsHit * fragDamage * distanceFactor);
            
            if (totalDamage > 0) {
                living.hurt(fragDamageSource, totalDamage);
                
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        entityPos.x, entityPos.y, entityPos.z,
                        fragmentsHit, 0.5, 0.5, 0.5, 0.1);
            }
        }
        
        serverLevel.playSound(null, BlockPos.containing(detonationPos), 
                SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0f, 0.5f);
    }
    
    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from).normalize();
        double distance = from.distanceTo(to);
        
        for (double d = 0.5; d < distance; d += 0.5) {
            Vec3 checkPos = from.add(direction.scale(d));
            BlockPos blockPos = BlockPos.containing(checkPos);
            
            if (this.level().getBlockState(blockPos).isSolidRender(this.level(), blockPos)) {
                return false;
            }
        }
        return true;
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
            this.level().addParticle(ParticleTypes.SMOKE,
                    pos.x, pos.y, pos.z,
                    (random.nextDouble() - 0.5) * 0.05,
                    (random.nextDouble() - 0.5) * 0.05,
                    (random.nextDouble() - 0.5) * 0.05);
        }
    }
}
