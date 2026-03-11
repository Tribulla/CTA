package com.cta.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class CBCCompat {
    
    private static final boolean CBC_LOADED;
    private static Class<?> fuzeItemClass = null;
    
    public static final int FUZE_IMPACT = 0;
    public static final int FUZE_TIMED = 1;
    public static final int FUZE_PROXIMITY = 2;
    public static final int FUZE_DELAY = 3;
    public static final int FUZE_UNKNOWN = -1;
    
    static {
        boolean loaded = false;
        try {
            fuzeItemClass = Class.forName("rbasamoyai.createbigcannons.munitions.fuzes.FuzeItem");
            loaded = true;
        } catch (ClassNotFoundException e) {
            loaded = false;
        }
        CBC_LOADED = loaded;
    }
    
    public static boolean isCBCLoaded() {
        return CBC_LOADED;
    }
    
    public static boolean isFuzeItem(Item item) {
        if (!CBC_LOADED || fuzeItemClass == null) {
            return false;
        }
        return fuzeItemClass.isInstance(item);
    }
    
    public static boolean isFuzeItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return isFuzeItem(stack.getItem());
    }
    
    public static String getFuzeTypeName(int fuzeType) {
        return switch (fuzeType) {
            case FUZE_IMPACT -> "Impact";
            case FUZE_TIMED -> "Timed";
            case FUZE_PROXIMITY -> "Proximity";
            case FUZE_DELAY -> "Delay";
            default -> "Unknown";
        };
    }
    
    public static int getFuzeType(ItemStack stack) {
        if (stack.isEmpty() || !isFuzeItem(stack)) {
            return FUZE_UNKNOWN;
        }
        
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return FUZE_UNKNOWN;
        
        String name = id.getPath().toLowerCase();
        
        if (name.contains("timed") || name.contains("time_fuze")) {
            return FUZE_TIMED;
        }
        
        if (name.contains("proximity") || name.contains("prox")) {
            return FUZE_PROXIMITY;
        }
        
        if (name.contains("dead_man") || name.contains("deadman") || name.contains("delay")) {
            return FUZE_DELAY;
        }
        
        if (name.contains("impact")) {
            return FUZE_IMPACT;
        }
        
        return FUZE_IMPACT;
    }
    
    public static int getTimedFuzeDuration(ItemStack stack) {
        if (stack.isEmpty()) return 100;
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("BlockEntityTag", 10)) {
                CompoundTag beTag = tag.getCompound("BlockEntityTag");
                if (beTag.contains("FuseTime")) {
                    return beTag.getInt("FuseTime") * 20;
                }
                if (beTag.contains("Timer")) {
                    return beTag.getInt("Timer");
                }
            }
            
            if (tag.contains("FuzeTimer")) {
                return tag.getInt("FuzeTimer");
            }
            if (tag.contains("FuseTime")) {
                return tag.getInt("FuseTime") * 20; 
            }
            if (tag.contains("Timer")) {
                return tag.getInt("Timer");
            }
            if (tag.contains("fuze_timer")) {
                return tag.getInt("fuze_timer");
            }
        }
        
        return 100;
    }
    
    public static double getProximityRange(ItemStack stack) {
        if (stack.isEmpty()) return 5.0;
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("ProximityRange")) {
                return tag.getDouble("ProximityRange");
            }
            if (tag.contains("Range")) {
                return tag.getDouble("Range");
            }
            if (tag.contains("DetectionRange")) {
                return tag.getDouble("DetectionRange");
            }
        }
        
        return 5.0;
    }
    
    public static int getDelayDuration(ItemStack stack) {
        if (stack.isEmpty()) return 10;
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("DelayTimer")) {
                return tag.getInt("DelayTimer");
            }
            if (tag.contains("Delay")) {
                return tag.getInt("Delay");
            }
            if (tag.contains("DetonationDelay")) {
                return tag.getInt("DetonationDelay");
            }
        }
        
        return 10;
    }
    
    private static Class<?> cannonMountClass = null;
    private static Method getYawMethod = null;
    private static Method getPitchMethod = null;
    private static boolean mountClassLoaded = false;
    
    private static void loadCannonMountClass() {
        if (mountClassLoaded) return;
        mountClassLoaded = true;
        
        if (!CBC_LOADED) return;
        
        try {
            cannonMountClass = Class.forName("rbasamoyai.createbigcannons.cannon.cannonmount.CannonMountBlockEntity");
            getYawMethod = cannonMountClass.getMethod("getYaw");
            getPitchMethod = cannonMountClass.getMethod("getPitch");
        } catch (Exception e) {
            try {
                cannonMountClass = Class.forName("rbasamoyai.createbigcannons.cannons.cannonmount.CannonMountBlockEntity");
                getYawMethod = cannonMountClass.getMethod("getYaw");
                getPitchMethod = cannonMountClass.getMethod("getPitch");
            } catch (Exception e2) {
                cannonMountClass = null;
            }
        }
    }
    
    public static boolean isCannonMount(Level level, BlockPos pos) {
        if (!CBC_LOADED) return false;
        loadCannonMountClass();
        if (cannonMountClass == null) return false;
        
        try {
            BlockEntity be = level.getBlockEntity(pos);
            return be != null && cannonMountClass.isInstance(be);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Nullable
    public static Float getCannonMountYaw(Level level, BlockPos pos) {
        if (!CBC_LOADED) return null;
        loadCannonMountClass();
        if (cannonMountClass == null || getYawMethod == null) return null;
        
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || !cannonMountClass.isInstance(be)) return null;
            
            Object result = getYawMethod.invoke(be);
            if (result instanceof Number num) {
                return num.floatValue();
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    @Nullable
    public static Float getCannonMountPitch(Level level, BlockPos pos) {
        if (!CBC_LOADED) return null;
        loadCannonMountClass();
        if (cannonMountClass == null || getPitchMethod == null) return null;
        
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || !cannonMountClass.isInstance(be)) return null;
            
            Object result = getPitchMethod.invoke(be);
            if (result instanceof Number num) {
                return num.floatValue();
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    public static double calculateRange(Level level, BlockPos mountPos, int charges, String shellType) {
        if (!CBC_LOADED) return -1;
        
        Float pitch = getCannonMountPitch(level, mountPos);
        if (pitch == null) return -1;
        
        double baseVelocity = getBaseVelocityForShell(shellType);
        double velocity = baseVelocity * charges;
        
        velocity = Math.min(velocity, 300.0);
        
        double angleRad = Math.toRadians(-pitch);
        
        double gravity = 0.05 * 20 * 20;
        
        double range = (velocity * velocity * Math.sin(2 * angleRad)) / gravity;
        
        return Math.max(0, range);
    }
    
    private static double getBaseVelocityForShell(String shellType) {
        if (shellType == null) return 20.0;
        
        return switch (shellType.toLowerCase()) {
            case "ap", "armor_piercing" -> 25.0;
            case "he", "high_explosive" -> 20.0;
            case "shot" -> 15.0;
            case "smoke" -> 18.0;
            case "flak" -> 22.0;
            case "mortar" -> 12.0;
            default -> 20.0;
        };
    }
    
    public static double calculateSimpleRange(Level level, BlockPos mountPos, int charges) {
        return calculateRange(level, mountPos, charges, "he");
    }
}
