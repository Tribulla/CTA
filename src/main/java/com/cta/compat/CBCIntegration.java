package com.cta.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public class CBCIntegration {
    
    private static Boolean cbcLoaded = null;
    
    public static boolean isCBCLoaded() {
        if (cbcLoaded == null) {
            cbcLoaded = ModList.get().isLoaded("createbigcannons");
        }
        return cbcLoaded;
    }
    
    public static double getBlockToughness(Level level, BlockState state, BlockPos pos) {
        if (isCBCLoaded()) {
            try {
                return CBCBlockArmorAccess.getToughness(level, state, pos);
            } catch (Throwable e) {
            }
        }
        return state.getBlock().getExplosionResistance();
    }
    
    public static double getBlockHardness(Level level, BlockState state, BlockPos pos) {
        if (isCBCLoaded()) {
            try {
                return CBCBlockArmorAccess.getHardness(level, state, pos);
            } catch (Throwable e) {
            }
        }
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed < 0) {
            return Double.MAX_VALUE;
        }
        return destroySpeed;
    }
    
    public static boolean canHeatPenetrate(Level level, BlockState state, BlockPos pos, double penetrationPower) {
        double toughness = getBlockToughness(level, state, pos);
        return penetrationPower > toughness;
    }
    
    public static boolean canFragmentDamage(Level level, BlockState state, BlockPos pos, double fragmentPower) {
        double toughness = getBlockToughness(level, state, pos);
        return fragmentPower > toughness;
    }
    
    public static double getPenetrationCost(Level level, BlockState state, BlockPos pos) {
        return getBlockToughness(level, state, pos);
    }
}
