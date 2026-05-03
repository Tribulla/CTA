package com.cta.compat.cc;

import com.cta.block.ScopeBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

public class ScopePeripheral implements IPeripheral {
    
    private final ScopeBlockEntity scope;

    public ScopePeripheral(ScopeBlockEntity scope) {
        this.scope = scope;
    }

    @Override
    public String getType() {
        return "cta_scope";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return this == other || (other instanceof ScopePeripheral && ((ScopePeripheral) other).scope == this.scope);
    }

    @LuaFunction
    public final float getYaw() {
        return scope.getScopeYaw();
    }

    @LuaFunction
    public final float getPitch() {
        return scope.getScopePitch();
    }

    @LuaFunction
    public final boolean isBoundToCannon() {
        return scope.getBoundCannonMount() != null;
    }
}