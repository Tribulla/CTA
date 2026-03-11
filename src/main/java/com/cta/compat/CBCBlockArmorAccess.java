package com.cta.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.reflect.Method;

public class CBCBlockArmorAccess {
    
    private static Class<?> handlerClass = null;
    private static Method getPropertiesMethod = null;
    private static Method toughnessMethod = null;
    private static Method hardnessMethod = null;
    private static boolean initialized = false;
    private static boolean available = false;
    
    private static void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            handlerClass = Class.forName("rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler");
            Class<?> providerClass = Class.forName("rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesProvider");
            
            getPropertiesMethod = handlerClass.getMethod("getProperties", BlockState.class);
            
            toughnessMethod = providerClass.getMethod("toughness", Level.class, BlockState.class, BlockPos.class, boolean.class);
            hardnessMethod = providerClass.getMethod("hardness", Level.class, BlockState.class, BlockPos.class, boolean.class);
            
            available = true;
        } catch (Throwable e) {
            available = false;
        }
    }
    
    public static boolean isAvailable() {
        init();
        return available;
    }
    
    public static double getToughness(Level level, BlockState state, BlockPos pos) {
        init();
        if (!available) {
            throw new RuntimeException("CBC not available");
        }
        
        try {
            Object provider = getPropertiesMethod.invoke(null, state);
            Object result = toughnessMethod.invoke(provider, level, state, pos, true);
            return ((Number) result).doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get CBC toughness", e);
        }
    }
    
    public static double getHardness(Level level, BlockState state, BlockPos pos) {
        init();
        if (!available) {
            throw new RuntimeException("CBC not available");
        }
        
        try {
            Object provider = getPropertiesMethod.invoke(null, state);
            Object result = hardnessMethod.invoke(provider, level, state, pos, true);
            return ((Number) result).doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get CBC hardness", e);
        }
    }
}
