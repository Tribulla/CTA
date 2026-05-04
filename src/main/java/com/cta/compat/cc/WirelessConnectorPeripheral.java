package com.cta.compat.cc;

import com.cta.wireless.WirelessLinkSavedData;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class WirelessConnectorPeripheral implements IPeripheral {

    private final BlockPos computerPos;
    private final ServerLevel level;

    public WirelessConnectorPeripheral(BlockPos computerPos, ServerLevel level) {
        this.computerPos = computerPos;
        this.level = level;
    }

    @Override
    public String getType() {
        return "wireless_connector";
    }

    @LuaFunction
    public final List<String> getChannels() {
        WirelessLinkSavedData data = WirelessLinkSavedData.get(level);
        return new ArrayList<>(data.getLinks(computerPos).keySet());
    }

    @LuaFunction
    public final int getPeripheralCount(String channel) throws LuaException {
        validateChannel(channel);
        Set<BlockPos> positions = WirelessLinkSavedData.get(level).getLinks(computerPos).get(channel);
        return positions != null ? positions.size() : 0;
    }

    @LuaFunction
    public final Map<String, Object> getPeripheralInfo(String channel, int index) throws LuaException {
        BlockPos pos = resolvePeripheralPos(channel, index);
        Map<String, Object> info = new HashMap<>();
        info.put("x", (double) pos.getX());
        info.put("y", (double) pos.getY());
        info.put("z", (double) pos.getZ());
        BlockState state = level.getBlockState(pos);
        info.put("block", ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());
        return info;
    }

    @LuaFunction
    public final Object getRemotePeripheral(String channel, int index) throws LuaException {
        BlockPos pos = resolvePeripheralPos(channel, index);

        // 1. Check if the remote block is a Scope
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof com.cta.block.ScopeBlockEntity scopeBE) {
            return new ScopePeripheral(scopeBE); // <-- Fixed this line!
        }

        // 2. Check if the remote block is a CBC Cannon Mount
        if (com.cta.compat.CBCCompat.isCBCLoaded() && com.cta.compat.CBCCompat.isCannonMount(level, pos)) {
            return new CBCCannonPeripheral(level, pos);
        }

        // 3. Fallback to native CC capabilities (Chests, Monitors, other CC mods)
        if (be != null) {
            net.minecraftforge.common.util.LazyOptional<IPeripheral> capability = be.getCapability(dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL);
            if (capability.isPresent()) {
                return capability.resolve().get();
            }
        }

        return null;
    }

    @LuaFunction
    public final Map<String, Object> getPeripheralData(String channel, int index) throws LuaException {
        BlockPos pos = resolvePeripheralPos(channel, index);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return Collections.emptyMap();
        CompoundTag tag = be.getUpdateTag();
        return nbtToMap(tag);
    }

    @LuaFunction
    public final void removeLink(String channel, int index) throws LuaException {
        BlockPos pos = resolvePeripheralPos(channel, index);
        WirelessLinkSavedData.get(level).removeLink(computerPos, channel, pos);
    }

    @LuaFunction
    public final void removeChannel(String channel) throws LuaException {
        validateChannel(channel);
        WirelessLinkSavedData.get(level).removeChannel(computerPos, channel);
    }


    private void validateChannel(String channel) throws LuaException {
        if (channel == null || channel.isEmpty()) throw new LuaException("Channel name required");
    }

    private BlockPos resolvePeripheralPos(String channel, int index) throws LuaException {
        validateChannel(channel);
        Set<BlockPos> positions = WirelessLinkSavedData.get(level).getLinks(computerPos).get(channel);
        if (positions == null || positions.isEmpty())
            throw new LuaException("No peripherals on channel '" + channel + "'");
        List<BlockPos> list = new ArrayList<>(positions);
        int i = index - 1;
        if (i < 0 || i >= list.size())
            throw new LuaException("Invalid index: " + index + " (count: " + list.size() + ")");
        return list.get(i);
    }

    private static Map<String, Object> nbtToMap(CompoundTag tag) {
        Map<String, Object> map = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            if (value != null) map.put(key, nbtToObject(value));
        }
        return map;
    }

    private static Object nbtToObject(Tag tag) {
        if (tag instanceof NumericTag numTag) {
            return numTag.getAsDouble();
        } else if (tag instanceof StringTag strTag) {
            return strTag.getAsString();
        } else if (tag instanceof CompoundTag compTag) {
            return nbtToMap(compTag);
        } else if (tag instanceof ListTag listTag) {
            List<Object> list = new ArrayList<>();
            for (Tag t : listTag) list.add(nbtToObject(t));
            return list;
        } else if (tag instanceof IntArrayTag intArr) {
            List<Object> list = new ArrayList<>();
            for (int v : intArr.getAsIntArray()) list.add((double) v);
            return list;
        } else if (tag instanceof LongArrayTag longArr) {
            List<Object> list = new ArrayList<>();
            for (long v : longArr.getAsLongArray()) list.add((double) v);
            return list;
        } else if (tag instanceof ByteArrayTag byteArr) {
            List<Object> list = new ArrayList<>();
            for (byte v : byteArr.getAsByteArray()) list.add((double) v);
            return list;
        }
        return tag.getAsString();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof WirelessConnectorPeripheral p &&
                p.computerPos.equals(this.computerPos);
    }
}
