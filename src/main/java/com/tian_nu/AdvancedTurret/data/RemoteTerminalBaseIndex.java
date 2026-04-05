package com.tian_nu.AdvancedTurret.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 远程终端基座索引（跨维度、含未加载区块）。
 */
public class RemoteTerminalBaseIndex extends SavedData {

    private static final String DATA_NAME = "advanced_turret_remote_terminal_base_index";

    private final Map<String, BaseRecord> records = new HashMap<>();

    public static RemoteTerminalBaseIndex get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available");
        }
        return overworld.getDataStorage().computeIfAbsent(
                RemoteTerminalBaseIndex::load,
                RemoteTerminalBaseIndex::new,
                DATA_NAME
        );
    }

    public static RemoteTerminalBaseIndex load(CompoundTag tag) {
        RemoteTerminalBaseIndex index = new RemoteTerminalBaseIndex();
        ListTag list = tag.getList("records", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag entryTag = (CompoundTag) element;
            String dimensionId = entryTag.getString("dimension");
            long posLong = entryTag.getLong("pos");
            UUID owner = entryTag.getUUID("owner");
            int tier = entryTag.getInt("tier");
            String baseName = entryTag.getString("name");
            if (baseName.isBlank()) {
                baseName = entryTag.getString("name_key");
            }
            if (baseName.isBlank()) {
                baseName = "Turret Base T" + Math.max(1, tier);
            }
            BaseRecord record = new BaseRecord(owner, dimensionId, posLong, tier, baseName);
            index.records.put(buildKey(dimensionId, posLong), record);
        }
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BaseRecord record : records.values()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("dimension", record.dimensionId());
            entryTag.putLong("pos", record.posLong());
            entryTag.putUUID("owner", record.owner());
            entryTag.putInt("tier", record.tier());
            entryTag.putString("name", record.baseName());
            list.add(entryTag);
        }
        tag.put("records", list);
        return tag;
    }

    public void upsert(ServerLevel level, BlockPos pos, UUID owner, int tier, String baseName) {
        if (owner == null) {
            return;
        }
        String dimensionId = level.dimension().location().toString();
        String key = buildKey(dimensionId, pos.asLong());
        BaseRecord next = new BaseRecord(owner, dimensionId, pos.asLong(), Math.max(1, tier),
                baseName == null || baseName.isBlank()
                        ? "Turret Base T" + Math.max(1, tier)
                        : baseName);
        BaseRecord old = records.put(key, next);
        if (!next.equals(old)) {
            setDirty();
        }
    }

    public void remove(ServerLevel level, BlockPos pos) {
        remove(level.dimension().location().toString(), pos);
    }

    public void remove(String dimensionId, BlockPos pos) {
        if (records.remove(buildKey(dimensionId, pos.asLong())) != null) {
            setDirty();
        }
    }

    public List<BaseRecord> getByOwner(UUID owner) {
        List<BaseRecord> result = new ArrayList<>();
        for (BaseRecord record : records.values()) {
            if (record.owner().equals(owner)) {
                result.add(record);
            }
        }
        return result;
    }

    private static String buildKey(String dimensionId, long posLong) {
        return dimensionId + "|" + posLong;
    }

    public record BaseRecord(UUID owner, String dimensionId, long posLong, int tier, String baseName) {
    }

}
