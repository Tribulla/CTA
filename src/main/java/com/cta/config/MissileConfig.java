package com.cta.config;

import com.cta.entity.MissileEntity.GuidanceType;
import com.cta.entity.MissileEntity.SteeringMethod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashMap;
import java.util.Map;

public class MissileConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHUNK_LOADING;
    public static final ForgeConfigSpec.IntValue MAX_FLIGHT_TICKS;
    public static final ForgeConfigSpec.DoubleValue MAX_MISSILE_SPEED;
    
    public static final ForgeConfigSpec.DoubleValue FRAGMENT_COUNT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FRAGMENT_BLOCK_DAMAGE_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue FRAGMENTS_BREAK_BLOCKS;
    
    public static final ForgeConfigSpec.DoubleValue HEAT_BASE_PENETRATION;
    public static final ForgeConfigSpec.BooleanValue USE_CBC_BLOCK_RESISTANCE;
    
    private static final Map<String, MissileTypeConfig> MISSILE_CONFIGS = new HashMap<>();
    
    static {
        BUILDER.comment("CTA Missile Configuration").push("general");
        
        ENABLE_CHUNK_LOADING = BUILDER
                .comment("Whether missiles should force-load chunks they fly through")
                .define("enableChunkLoading", true);
        
        MAX_FLIGHT_TICKS = BUILDER
                .comment("Maximum ticks a missile can exist before auto-removing (0 = infinite)")
                .defineInRange("maxFlightTicks", 0, 0, Integer.MAX_VALUE);
        
        MAX_MISSILE_SPEED = BUILDER
                .comment("Maximum missile speed in blocks per tick (20 ticks/sec).",
                        "3.0 = 60 blocks/sec. Higher values cause chunk loading lag.")
                .defineInRange("maxMissileSpeed", 5.0, 0.5, 20.0);
        
        BUILDER.pop();
        
        BUILDER.comment("Fragment/Shrapnel Configuration").push("fragments");
        
        FRAGMENT_COUNT_MULTIPLIER = BUILDER
                .comment("Multiplier for fragment count from HEFRAG warheads (1.0 = normal, 2.0 = double)")
                .defineInRange("fragmentCountMultiplier", 1.0, 0.1, 5.0);
        
        FRAGMENTS_BREAK_BLOCKS = BUILDER
                .comment("Whether fragments can break weak blocks")
                .define("fragmentsBreakBlocks", true);
        
        FRAGMENT_BLOCK_DAMAGE_THRESHOLD = BUILDER
                .comment("Maximum block toughness (CBC) or explosion resistance (vanilla) that fragments can break",
                        "Blocks with toughness/resistance below this value can be destroyed by fragments")
                .defineInRange("fragmentBlockDamageThreshold", 6.0, 0.0, 100.0);
        
        BUILDER.pop();
        
        BUILDER.comment("HEAT Warhead Configuration").push("heat");
        
        HEAT_BASE_PENETRATION = BUILDER
                .comment("Base penetration power for HEAT warheads (compared against block toughness)")
                .defineInRange("basePenetration", 50.0, 1.0, 500.0);
        
        USE_CBC_BLOCK_RESISTANCE = BUILDER
                .comment("Use Create Big Cannons block armor system for penetration calculations",
                        "When CBC is installed and this is true, uses CBC's hardness/toughness values",
                        "When CBC is not installed or this is false, uses vanilla explosion resistance")
                .define("useCBCBlockResistance", true);
        
        BUILDER.pop();
        
        BUILDER.push("missiles");
        
        // Parameters: id, category, fuel, thrust, explosion, gravity, airRes, initSpeed, drop, airbrake, glide,
        //             guidance, steeringMethod, seekerFov, seekerRange, seekerMinTemp, turnRate, navGain
        // Steering methods: PURE_PURSUIT, PROPORTIONAL_NAV, LEAD_PURSUIT, COMMAND_LOS
        registerMissileConfig("3m54_kalibr", MissileCategory.CRUISE_MISSILE, 
                200, 0.08, 4.0f, 0.02, 0.99, 0.8, 0.0, false, 2.0,
                GuidanceType.NONE, SteeringMethod.PURE_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("agm_88", MissileCategory.MISSILE, 
                180, 0.12, 5.0f, 0.03, 0.98, 1.0, 0.0, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PROPORTIONAL_NAV, 0, 0, 0, 0, 4.0);
        registerMissileConfig("aim_9", MissileCategory.MISSILE, 
                150, 0.15, 4.0f, 0.04, 0.98, 1.2, 0.0, false, 0.0,
                GuidanceType.HEAT_SEEKING, SteeringMethod.PROPORTIONAL_NAV, 45, 512, 100, 5, 4.0);
        registerMissileConfig("apkws", MissileCategory.ROCKET, 
                80, 0.15, 3.0f, 0.05, 0.97, 0.8, 0.02, false, 0.0,
                GuidanceType.NONE, SteeringMethod.LEAD_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("hellfire", MissileCategory.MISSILE, 
                140, 0.12, 6.0f, 0.03, 0.98, 1.0, 0.0, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PROPORTIONAL_NAV, 0, 0, 0, 0, 4.0);
        registerMissileConfig("hydra_70", MissileCategory.ROCKET, 
                60, 0.18, 2.5f, 0.06, 0.96, 0.7, 0.03, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PURE_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("katyusha", MissileCategory.ROCKET, 
                100, 0.14, 4.0f, 0.05, 0.97, 0.8, 0.025, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PURE_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("r_77", MissileCategory.MISSILE, 
                160, 0.12, 5.0f, 0.03, 0.98, 1.2, 0.0, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PROPORTIONAL_NAV, 0, 0, 0, 0, 4.0);
        registerMissileConfig("tow_2", MissileCategory.MISSILE, 
                120, 0.08, 5.5f, 0.02, 0.99, 0.5, 0.0, false, 0.0,
                GuidanceType.NONE, SteeringMethod.COMMAND_LOS, 0, 0, 0, 3, 3.0);
        registerMissileConfig("cbu_87", MissileCategory.CLUSTER_BOMB, 
                0, 0.0, 3.0f, 0.05, 0.98, 0.1, 0.0, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PURE_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("gbu_12", MissileCategory.GUIDED_BOMB, 
                0, 0.0, 6.0f, 0.04, 0.98, 0.15, 0.0, false, 1.5,
                GuidanceType.NONE, SteeringMethod.LEAD_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("gbu_24", MissileCategory.GUIDED_BOMB, 
                0, 0.0, 8.0f, 0.04, 0.97, 0.15, 0.0, false, 1.8,
                GuidanceType.NONE, SteeringMethod.LEAD_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("m64", MissileCategory.BOMB, 
                0, 0.0, 5.0f, 0.05, 0.98, 0.1, 0.0, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PURE_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("rockeye", MissileCategory.CLUSTER_BOMB, 
                0, 0.0, 3.5f, 0.05, 0.98, 0.1, 0.0, false, 0.0,
                GuidanceType.NONE, SteeringMethod.PURE_PURSUIT, 0, 0, 0, 0, 3.0);
        registerMissileConfig("snakeye", MissileCategory.RETARDED_BOMB, 
                0, 0.0, 4.0f, 0.05, 0.85, 0.1, 0.0, true, 0.0,
                GuidanceType.NONE, SteeringMethod.PURE_PURSUIT, 0, 0, 0, 0, 3.0);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
    
    private static void registerMissileConfig(String id, MissileCategory category,
            int fuelTicks, double thrust, float explosionPower, double gravity,
            double airResistance, double initialSpeed, double dropRate,
            boolean hasAirbrake, double glideRatio,
            GuidanceType guidanceType, SteeringMethod steeringMethod,
            double seekerFov, double seekerRange,
            double seekerMinTemp, double turnRate, double navGain) {
        
        BUILDER.comment("Configuration for " + id).push(id);
        
        MissileTypeConfig config = new MissileTypeConfig();
        
        config.category = category;
        config.guidanceTypeDefault = guidanceType;
        config.steeringMethodDefault = steeringMethod;
        
        config.fuelTicks = BUILDER
                .comment("Ticks of fuel (thrust duration). 0 for bombs.")
                .defineInRange("fuelTicks", fuelTicks, 0, 6000);
        
        config.thrust = BUILDER
                .comment("Thrust acceleration per tick while fuel remains")
                .defineInRange("thrust", thrust, 0.0, 2.0);
        
        config.explosionPower = BUILDER
                .comment("Explosion power on impact (TNT = 4.0)")
                .defineInRange("explosionPower", explosionPower, 0.0, 100.0);
        
        config.gravity = BUILDER
                .comment("Gravity acceleration per tick")
                .defineInRange("gravity", gravity, 0.0, 1.0);
        
        config.airResistance = BUILDER
                .comment("Air resistance multiplier per tick (0.99 = 1% speed loss)")
                .defineInRange("airResistance", airResistance, 0.5, 1.0);
        
        config.initialSpeed = BUILDER
                .comment("Initial launch speed multiplier")
                .defineInRange("initialSpeed", initialSpeed, 0.0, 10.0);
        
        config.dropRate = BUILDER
                .comment("Additional drop rate during thrust (for rockets)")
                .defineInRange("dropRate", dropRate, 0.0, 0.5);
        
        config.hasAirbrake = BUILDER
                .comment("Whether this munition has airbrakes (retarded bombs)")
                .define("hasAirbrake", hasAirbrake);
        
        config.glideRatio = BUILDER
                .comment("Glide ratio for winged missiles (0 = no glide, higher = more glide)")
                .defineInRange("glideRatio", glideRatio, 0.0, 10.0);
        
        config.armorPenetration = BUILDER
                .comment("Armor penetration in blocks (for HEAT warhead type)")
                .defineInRange("armorPenetration", 5.0, 0.0, 50.0);
        
        config.fragCount = BUILDER
                .comment("Number of fragments on detonation (for HEFRAG warhead type)")
                .defineInRange("fragCount", 
                        category == MissileCategory.CLUSTER_BOMB ? 20 : 15, 
                        0, 100);
        
        config.fragDamage = BUILDER
                .comment("Damage per fragment")
                .defineInRange("fragDamage", 4.0, 0.0, 50.0);
        
        config.fragRange = BUILDER
                .comment("Fragment spread range in blocks")
                .defineInRange("fragRange", 10.0, 0.0, 50.0);
        
        if (guidanceType != GuidanceType.NONE) {
            config.guidanceTypeStr = BUILDER
                    .comment("Guidance type: NONE, HEAT_SEEKING")
                    .define("guidanceType", guidanceType.name());
            
            config.steeringMethodStr = BUILDER
                    .comment("Steering method: PURE_PURSUIT, PROPORTIONAL_NAV, LEAD_PURSUIT, COMMAND_LOS",
                            "PURE_PURSUIT: Steer directly at target (simple tail chase)",
                            "PROPORTIONAL_NAV: Null LOS rate for straight intercept (AIM-9, R-77)",
                            "LEAD_PURSUIT: Predict target position and aim ahead",
                            "COMMAND_LOS: Stay on launcher-to-target line (TOW, wire-guided)")
                    .define("steeringMethod", steeringMethod.name());
            
            config.seekerFov = BUILDER
                    .comment("Seeker field of view (half-angle in degrees)")
                    .defineInRange("seekerFov", seekerFov, 5.0, 90.0);
            
            config.seekerRange = BUILDER
                    .comment("Maximum seeker detection range in blocks (increase for DH/VS combat)")
                    .defineInRange("seekerRange", seekerRange, 10.0, 2048.0);
            
            config.seekerMinTemp = BUILDER
                    .comment("Minimum temperature (°C) for heat seeker to track")
                    .defineInRange("seekerMinTemp", seekerMinTemp, 0.0, 3000.0);
            
            config.turnRate = BUILDER
                    .comment("Maximum degrees per tick the missile can turn")
                    .defineInRange("turnRate", turnRate, 1.0, 45.0);
            
            config.navGain = BUILDER
                    .comment("Navigation gain (N) for Proportional Navigation steering.",
                            "Higher = more aggressive intercept. Typical: 3-5. Only used with PROPORTIONAL_NAV.")
                    .defineInRange("navGain", navGain, 1.0, 10.0);
        }
        
        BUILDER.pop();
        
        MISSILE_CONFIGS.put(id, config);
    }
    
    public static MissileTypeConfig getConfig(String missileId) {
        return MISSILE_CONFIGS.getOrDefault(missileId, getDefaultConfig());
    }
    
    private static MissileTypeConfig getDefaultConfig() {
        MissileTypeConfig defaultConfig = new MissileTypeConfig();
        defaultConfig.category = MissileCategory.MISSILE;
        return defaultConfig;
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "cta-missiles.toml");
    }
    
    public enum MissileCategory {
        MISSILE,
        ROCKET,
        CRUISE_MISSILE,
        BOMB,
        GUIDED_BOMB,
        RETARDED_BOMB,
        CLUSTER_BOMB //integrate later
    }
    
    public static class MissileTypeConfig {
        public MissileCategory category;
        public GuidanceType guidanceTypeDefault = GuidanceType.NONE;
        public SteeringMethod steeringMethodDefault = SteeringMethod.PROPORTIONAL_NAV;
        public ForgeConfigSpec.IntValue fuelTicks;
        public ForgeConfigSpec.DoubleValue thrust;
        public ForgeConfigSpec.DoubleValue explosionPower;
        public ForgeConfigSpec.DoubleValue gravity;
        public ForgeConfigSpec.DoubleValue airResistance;
        public ForgeConfigSpec.DoubleValue initialSpeed;
        public ForgeConfigSpec.DoubleValue dropRate;
        public ForgeConfigSpec.BooleanValue hasAirbrake;
        public ForgeConfigSpec.DoubleValue glideRatio;
        public ForgeConfigSpec.DoubleValue armorPenetration;
        public ForgeConfigSpec.IntValue fragCount;
        public ForgeConfigSpec.DoubleValue fragDamage;
        public ForgeConfigSpec.DoubleValue fragRange;
        
        public ForgeConfigSpec.ConfigValue<String> guidanceTypeStr;
        public ForgeConfigSpec.ConfigValue<String> steeringMethodStr;
        public ForgeConfigSpec.DoubleValue seekerFov;
        public ForgeConfigSpec.DoubleValue seekerRange;
        public ForgeConfigSpec.DoubleValue seekerMinTemp;
        public ForgeConfigSpec.DoubleValue turnRate;
        public ForgeConfigSpec.DoubleValue navGain;
        
        public int getFuelTicks() { return fuelTicks != null ? fuelTicks.get() : 120; }
        public double getThrust() { return thrust != null ? thrust.get() : 0.15; }
        public float getExplosionPower() { return explosionPower != null ? explosionPower.get().floatValue() : 4.0f; }
        public double getGravity() { return gravity != null ? gravity.get() : 0.04; }
        public double getAirResistance() { return airResistance != null ? airResistance.get() : 0.99; }
        public double getInitialSpeed() { return initialSpeed != null ? initialSpeed.get() : 0.8; }
        public double getDropRate() { return dropRate != null ? dropRate.get() : 0.0; }
        public boolean hasAirbrake() { return hasAirbrake != null && hasAirbrake.get(); }
        public double getGlideRatio() { return glideRatio != null ? glideRatio.get() : 0.0; }
        public double getArmorPenetration() { return armorPenetration != null ? armorPenetration.get() : 5.0; }
        public int getFragCount() { return fragCount != null ? fragCount.get() : 15; }
        public double getFragDamage() { return fragDamage != null ? fragDamage.get() : 4.0; }
        public double getFragRange() { return fragRange != null ? fragRange.get() : 10.0; }
        
        public GuidanceType getGuidanceType() {
            if (guidanceTypeStr != null) {
                try {
                    return GuidanceType.valueOf(guidanceTypeStr.get());
                } catch (IllegalArgumentException e) {
                    return guidanceTypeDefault;
                }
            }
            return guidanceTypeDefault;
        }
        public double getSeekerFov() { return seekerFov != null ? seekerFov.get() : 45.0; }
        public double getSeekerRange() { return seekerRange != null ? seekerRange.get() : 64.0; }
        public double getSeekerMinTemp() { return seekerMinTemp != null ? seekerMinTemp.get() : 100.0; }
        public double getTurnRate() { return turnRate != null ? turnRate.get() : 15.0; }
        public double getNavGain() { return navGain != null ? navGain.get() : 4.0; }
        public SteeringMethod getSteeringMethod() {
            if (steeringMethodStr != null) {
                try {
                    return SteeringMethod.valueOf(steeringMethodStr.get());
                } catch (IllegalArgumentException e) {
                    return steeringMethodDefault;
                }
            }
            return steeringMethodDefault;
        }
    }
}
