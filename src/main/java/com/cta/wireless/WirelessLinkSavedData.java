package com.cta.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class WirelessLinkSavedData extends SavedData {

    private static final String DATA_NAME = "cta_wireless_links";

    private final Map<BlockPos, Map<String, Set<BlockPos>>> links = new HashMap<>();

    public WirelessLinkSavedData() {}

    public static WirelessLinkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WirelessLinkSavedData::load, WirelessLinkSavedData::new,
                DATA_NAME
        );
    }

    public void addLink(BlockPos target, String channel, BlockPos peripheral) {
        links.computeIfAbsent(target, k -> new HashMap<>())
                .computeIfAbsent(channel, k -> new LinkedHashSet<>())
                .add(peripheral);
        setDirty();
    }

    public void removeLink(BlockPos target, String channel, BlockPos peripheral) {
        Map<String, Set<BlockPos>> channelMap = links.get(target);
        if (channelMap == null) return;
        Set<BlockPos> set = channelMap.get(channel);
        if (set == null) return;
        set.remove(peripheral);
        if (set.isEmpty()) channelMap.remove(channel);
        if (channelMap.isEmpty()) links.remove(target);
        setDirty();
    }

    public void removeChannel(BlockPos target, String channel) {
        Map<String, Set<BlockPos>> channelMap = links.get(target);
        if (channelMap == null) return;
        channelMap.remove(channel);
        if (channelMap.isEmpty()) links.remove(target);
        setDirty();
    }

    public boolean hasLinks(BlockPos target) {
        Map<String, Set<BlockPos>> channelMap = links.get(target);
        return channelMap != null && !channelMap.isEmpty();
    }

    public Map<String, Set<BlockPos>> getLinks(BlockPos target) {
        return links.getOrDefault(target, Collections.emptyMap());
    }

    public int removeAllLinksForPeripheral(BlockPos peripheral) {
        int removed = 0;
        Iterator<Map.Entry<BlockPos, Map<String, Set<BlockPos>>>> targetIt = links.entrySet().iterator();
        while (targetIt.hasNext()) {
            Map.Entry<BlockPos, Map<String, Set<BlockPos>>> targetEntry = targetIt.next();
            Iterator<Map.Entry<String, Set<BlockPos>>> channelIt = targetEntry.getValue().entrySet().iterator();
            while (channelIt.hasNext()) {
                Map.Entry<String, Set<BlockPos>> channelEntry = channelIt.next();
                if (channelEntry.getValue().remove(peripheral)) {
                    removed++;
                    if (channelEntry.getValue().isEmpty()) channelIt.remove();
                }
            }
            if (targetEntry.getValue().isEmpty()) targetIt.remove();
        }
        if (removed > 0) setDirty();
        return removed;
    }

    public boolean isPeripheral(BlockPos peripheral) {
        for (Map<String, Set<BlockPos>> channelMap : links.values()) {
            for (Set<BlockPos> posSet : channelMap.values()) {
                if (posSet.contains(peripheral)) return true;
            }
        }
        return false;
    }

    public static WirelessLinkSavedData load(CompoundTag tag) {
        WirelessLinkSavedData data = new WirelessLinkSavedData();
        CompoundTag linksTag = tag.getCompound("Links");
        for (String targetKey : linksTag.getAllKeys()) {
            BlockPos target = BlockPos.of(Long.parseLong(targetKey));
            CompoundTag channelsTag = linksTag.getCompound(targetKey);
            Map<String, Set<BlockPos>> channelMap = new HashMap<>();
            for (String channel : channelsTag.getAllKeys()) {
                long[] positions = channelsTag.getLongArray(channel);
                Set<BlockPos> posSet = new LinkedHashSet<>();
                for (long pos : positions) {
                    posSet.add(BlockPos.of(pos));
                }
                channelMap.put(channel, posSet);
            }
            data.links.put(target, channelMap);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag linksTag = new CompoundTag();
        for (var entry : links.entrySet()) {
            CompoundTag channelsTag = new CompoundTag();
            for (var channelEntry : entry.getValue().entrySet()) {
                long[] positions = channelEntry.getValue().stream()
                        .mapToLong(BlockPos::asLong).toArray();
                channelsTag.putLongArray(channelEntry.getKey(), positions);
            }
            linksTag.put(String.valueOf(entry.getKey().asLong()), channelsTag);
        }
        tag.put("Links", linksTag);
        return tag;
    }
}
