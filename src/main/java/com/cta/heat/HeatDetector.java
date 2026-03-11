package com.cta.heat;

import com.Tribulla.thermodynamica.api.HeatAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class HeatDetector {

    public static final double FREEZING = -20.0;
    public static final double COLD = 0.0;
    public static final double COOL = 10.0;
    public static final double AMBIENT = 20.0;
    public static final double WARM = 100.0;
    public static final double HOT = 500.0;
    public static final double VERY_HOT = 1000.0;
    public static final double EXTREME = 3000.0;
    public static final double SUN = 5000.0;

    public static double getBlockHeat(Level level, BlockPos pos) {
        return HeatAPI.get().getVisualCelsius(level, pos);
    }

    public static double getEntityHeat(Entity entity) {
        if (entity == null) {
            return AMBIENT;
        }

        if (entity.isOnFire()) {
            return VERY_HOT;
        }

        if (entity instanceof LivingEntity living) {
            double heat = WARM;

            if (living instanceof Player player) {
                if (player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) {
                    heat *= 1.5;
                }
            }

            String entityType = entity.getType().toString().toLowerCase();
            if (entityType.contains("blaze") || entityType.contains("magma")) {
                heat = EXTREME;
            } else if (entityType.contains("ghast") || entityType.contains("strider")) {
                heat = HOT;
            }

            return heat;
        }

        if (entity instanceof Projectile) {
            Vec3 velocity = entity.getDeltaMovement();
            double speed = velocity.length();
            if (speed > 0.5) {
                return WARM * Math.min(speed, 2.0) * 0.5;
            }
        }

        if (entity instanceof ItemEntity) {
            return AMBIENT;
        }

        return AMBIENT;
    }

    public static List<HeatSignature> detectHeatInRange(Level level, Vec3 center, double range, double minHeat) {
        List<HeatSignature> signatures = new ArrayList<>();

        AABB searchBox = new AABB(
                center.x - range, center.y - range, center.z - range,
                center.x + range, center.y + range, center.z + range);

        for (Entity entity : level.getEntities(null, searchBox)) {
            double heat = getEntityHeat(entity);
            if (heat >= minHeat) {
                double distance = entity.position().distanceTo(center);
                if (distance <= range) {
                    signatures.add(new HeatSignature(
                            entity.position(),
                            heat,
                            SignatureType.ENTITY,
                            entity));
                }
            }
        }

        BlockPos centerPos = BlockPos.containing(center);
        int blockRange = (int) Math.min(range, 32);

        for (int x = -blockRange; x <= blockRange; x++) {
            for (int y = -blockRange; y <= blockRange; y++) {
                for (int z = -blockRange; z <= blockRange; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq > range * range)
                        continue;

                    BlockPos checkPos = centerPos.offset(x, y, z);
                    double heat = getBlockHeat(level, checkPos);

                    if (heat >= minHeat) {
                        signatures.add(new HeatSignature(
                                Vec3.atCenterOf(checkPos),
                                heat,
                                SignatureType.BLOCK,
                                null));
                    }
                }
            }
        }

        signatures.sort((a, b) -> Double.compare(b.heat, a.heat));

        return signatures;
    }

    public static boolean hasHeatAt(Level level, Vec3 pos, double minHeat) {
        BlockPos blockPos = BlockPos.containing(pos);
        if (getBlockHeat(level, blockPos) >= minHeat) {
            return true;
        }

        AABB box = new AABB(pos.x - 1, pos.y - 1, pos.z - 1, pos.x + 1, pos.y + 1, pos.z + 1);
        for (Entity entity : level.getEntities(null, box)) {
            if (getEntityHeat(entity) >= minHeat) {
                return true;
            }
        }

        return false;
    }

    public static HeatSignature findHottestInCone(Level level, Vec3 origin, Vec3 direction,
            double range, double coneAngle, double minHeat) {
        List<HeatSignature> allSignatures = detectHeatInRange(level, origin, range, minHeat);

        double cosAngle = Math.cos(Math.toRadians(coneAngle));
        HeatSignature hottest = null;

        for (HeatSignature sig : allSignatures) {
            Vec3 toTarget = sig.position.subtract(origin).normalize();
            double dot = direction.dot(toTarget);

            if (dot >= cosAngle) {
                if (hottest == null || sig.heat > hottest.heat) {
                    hottest = sig;
                }
            }
        }

        return hottest;
    }

    public static double getSunHeat(Level level, Vec3 lookDirection) {
        if (level.isClientSide) {
            long dayTime = level.getDayTime() % 24000;
            boolean isDaytime = dayTime < 12000;

            if (!isDaytime) {
                return AMBIENT;
            }

            float sunAngle = level.getSunAngle(1.0f);

            double sunYaw = sunAngle * 2 * Math.PI;
            Vec3 sunDirection = new Vec3(
                    -Math.sin(sunYaw),
                    Math.cos(sunYaw),
                    0).normalize();

            double dot = lookDirection.dot(sunDirection);
            if (dot > 0.996) {
                return SUN;
            } else if (dot > 0.95) {
                double factor = (dot - 0.95) / (0.996 - 0.95);
                return SUN * factor;
            }
        }
        return AMBIENT;
    }

    public static double getCloudAttenuation(Level level, Vec3 from, Vec3 to) {
        double cloudLayerMin = 192;
        double cloudLayerMax = 220;

        double minY = Math.min(from.y, to.y);
        double maxY = Math.max(from.y, to.y);

        if (maxY < cloudLayerMin || minY > cloudLayerMax) {
            return 1.0;
        }

        double weatherFactor = 1.0;
        if (level.isRaining()) {
            weatherFactor = 0.6;
        }
        if (level.isThundering()) {
            weatherFactor = 0.3;
        }

        double pathLength = from.distanceTo(to);
        if (pathLength < 0.1)
            return 1.0;

        double cloudPathLength = 0;
        if (minY < cloudLayerMin && maxY > cloudLayerMin) {
            cloudPathLength = Math.min(maxY, cloudLayerMax) - cloudLayerMin;
        } else if (minY >= cloudLayerMin && maxY <= cloudLayerMax) {
            cloudPathLength = maxY - minY;
        } else if (minY < cloudLayerMax && maxY > cloudLayerMax) {
            cloudPathLength = cloudLayerMax - Math.max(minY, cloudLayerMin);
        }

        double cloudAttenuation = Math.pow(0.95, cloudPathLength);

        return cloudAttenuation * weatherFactor;
    }

    public static double getAmbientTemperature(Level level, BlockPos pos) {
        double temp = AMBIENT;

        float biomeTemp = level.getBiome(pos).value().getBaseTemperature();
        if (biomeTemp < 0.2f) {
            temp = COOL + (biomeTemp * 50);
        } else if (biomeTemp > 1.0f) {
            temp = WARM * 0.3;
        }

        if (!level.canSeeSky(pos)) {
            temp -= 5;
        }

        if (level.canSeeSky(pos) && level.isDay() && !level.isRaining()) {
            temp += 5;
        }

        if (level.isRaining() && level.canSeeSky(pos)) {
            temp -= 10;
        }

        return temp;
    }

    public static class HeatSignature {
        public final Vec3 position;
        public final double heat;
        public final SignatureType type;
        public final Entity entity;
        public final AABB bounds;

        public HeatSignature(Vec3 position, double heat, SignatureType type, Entity entity) {
            this.position = position;
            this.heat = heat;
            this.type = type;
            this.entity = entity;
            this.bounds = entity != null ? entity.getBoundingBox() : new AABB(BlockPos.containing(position));
        }

        public HeatSignature(Vec3 position, double heat, SignatureType type, Entity entity, AABB bounds) {
            this.position = position;
            this.heat = heat;
            this.type = type;
            this.entity = entity;
            this.bounds = bounds;
        }
    }

    public enum SignatureType {
        ENTITY,
        BLOCK,
        SUN
    }
}
