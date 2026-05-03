package com.cta.compat.cc;

import com.cta.compat.CBCCompat;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class CBCCannonPeripheral implements IPeripheral {
    private final Level level;
    private final BlockPos pos;

    public CBCCannonPeripheral(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Override
    public String getType() {
        return "cbc_cannon";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof CBCCannonPeripheral && ((CBCCannonPeripheral) other).pos.equals(this.pos);
    }

    @LuaFunction
    public final float getYaw() {
        Float yaw = CBCCompat.getCannonMountYaw(level, pos);
        return yaw != null ? yaw : 0f;
    }

    @LuaFunction
    public final float getPitch() {
        Float pitch = CBCCompat.getCannonMountPitch(level, pos);
        return pitch != null ? pitch : 0f;
    }

    @LuaFunction
    public final Map<String, Object> getRangeData() {
        Map<String, Object> result = new HashMap<>();
        CBCCompat.CannonRangeData data = CBCCompat.getCannonRangeData(level, pos);
        if (data != null) {
            result.put("shell", data.shellName());
            result.put("currentRange", data.currentRange());
            result.put("loadedCharges", data.loadedChargeLevels());
        }
        return result;
    }
}