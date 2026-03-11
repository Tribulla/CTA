package com.cta.entity;

import com.cta.compat.CBCIntegration;
import com.cta.config.MissileConfig;
import com.cta.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class FragmentEntity extends Projectile {
    
    private float damage = 4.0f;
    private int lifeTime = 0;
    private static final int MAX_LIFETIME = 40;
    private boolean hasHit = false;
    
    public FragmentEntity(EntityType<? extends FragmentEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }
    
    public FragmentEntity(Level level, double x, double y, double z) {
        this(ModEntities.FRAGMENT.get(), level);
        this.setPos(x, y, z);
    }
    
    public static FragmentEntity create(Level level, Vec3 pos, Vec3 velocity, float damage) {
        FragmentEntity fragment = new FragmentEntity(level, pos.x, pos.y, pos.z);
        fragment.setDeltaMovement(velocity);
        fragment.damage = damage;
        return fragment;
    }
    
    @Override
    protected void defineSynchedData() {
    }
    
    @Override
    public void tick() {
        super.tick();
        
        lifeTime++;
        
        if (lifeTime > MAX_LIFETIME) {
            this.discard();
            return;
        }
        
        if (hasHit) {
            this.discard();
            return;
        }
        
        Vec3 motion = this.getDeltaMovement();
        motion = motion.add(0, -0.02, 0);
        motion = motion.scale(0.98);
        this.setDeltaMovement(motion);
        
        Vec3 oldPos = this.position();
        this.move(MoverType.SELF, motion);
        
        if (!this.level().isClientSide) {
            checkEntityCollision(oldPos);
        }
        
        if (this.horizontalCollision || this.verticalCollision) {
            onHitBlock();
        }
        
        if (this.level().isClientSide && lifeTime % 2 == 0) {
            this.level().addParticle(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0);
        }
    }
    
    private void checkEntityCollision(Vec3 startPos) {
        Vec3 endPos = this.position();
        AABB searchBox = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(0.5);
        
        List<Entity> entities = this.level().getEntities(this, searchBox, 
                e -> e instanceof LivingEntity && e.isAlive() && e != this.getOwner());
        
        for (Entity entity : entities) {
            if (entity.getBoundingBox().inflate(0.3).contains(this.position())) {
                onHitEntity(entity);
                return;
            }
        }
    }
    
    private void onHitEntity(Entity target) {
        if (hasHit) return;
        hasHit = true;
        
        if (!this.level().isClientSide && target instanceof LivingEntity living) {
            DamageSource damageSource = this.level().damageSources().thrown(this, this.getOwner());
            living.hurt(damageSource, damage);
            
            Vec3 knockback = this.getDeltaMovement().normalize().scale(0.3);
            living.push(knockback.x, knockback.y + 0.1, knockback.z);
            
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        3, 0.2, 0.2, 0.2, 0.1);
            }
        }
        
        this.discard();
    }
    
    private void onHitBlock() {
        if (hasHit) return;
        hasHit = true;
        
        if (!this.level().isClientSide) {
            BlockPos hitPos = this.blockPosition();
            BlockState state = this.level().getBlockState(hitPos);
            
            if (!state.isAir() && MissileConfig.FRAGMENTS_BREAK_BLOCKS.get()) {
                double velocity = this.getDeltaMovement().length();
                double fragmentPower = damage * velocity * 0.5;
                
                double threshold = MissileConfig.FRAGMENT_BLOCK_DAMAGE_THRESHOLD.get();
                
                double blockToughness;
                if (MissileConfig.USE_CBC_BLOCK_RESISTANCE.get()) {
                    blockToughness = CBCIntegration.getBlockToughness(this.level(), state, hitPos);
                } else {
                    blockToughness = state.getBlock().getExplosionResistance();
                }
            
                boolean canBreak = blockToughness <= threshold || 
                                   (canBreakBlock(state) && fragmentPower > blockToughness);
                
                if (canBreak && blockToughness >= 0) {
                    this.level().destroyBlock(hitPos, true);
                    
                    this.level().playSound(null, hitPos, state.getSoundType().getBreakSound(), 
                            SoundSource.BLOCKS, 0.5f, 1.2f);
                } else {
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT,
                                this.getX(), this.getY(), this.getZ(),
                                2, 0.1, 0.1, 0.1, 0.05);
                    }
                    
                    this.level().playSound(null, hitPos, SoundEvents.CHAIN_HIT, 
                            SoundSource.BLOCKS, 0.3f, 1.5f + random.nextFloat() * 0.5f);
                }
            }
        }
        
        this.discard();
    }
    
    private boolean canBreakBlock(BlockState state) {
        if (state.is(BlockTags.IMPERMEABLE) || state.getBlock() == Blocks.GLASS || 
            state.getBlock() == Blocks.GLASS_PANE) {
            return true;
        }
        
        if (state.is(BlockTags.LEAVES)) {
            return true;
        }
        
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) {
            return true;
        }
        
        if (state.is(BlockTags.CANDLES) || state.is(BlockTags.FLOWER_POTS)) {
            return true;
        }
        
        if (state.getBlock() == Blocks.TORCH || state.getBlock() == Blocks.WALL_TORCH ||
            state.getBlock() == Blocks.LANTERN || state.getBlock() == Blocks.SOUL_LANTERN) {
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        onHitEntity(result.getEntity());
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
        onHitBlock();
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        this.lifeTime = tag.getInt("LifeTime");
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("LifeTime", lifeTime);
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
