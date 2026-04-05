package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.data.RemoteTerminalBaseIndex;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 请求本人炮塔基座列表（含跨维度与未加载区块）。
 */
public record RemoteTerminalQueryPacket() {

    private static final int SEARCH_CHUNK_RADIUS = 12;

    public static void encode(RemoteTerminalQueryPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(true);
    }

    public static RemoteTerminalQueryPacket decode(FriendlyByteBuf buf) {
        if (buf.isReadable()) {
            buf.readBoolean();
        }
        return new RemoteTerminalQueryPacket();
    }

    public static void handle(RemoteTerminalQueryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            sendBaseListToPlayer(player);
        });
        context.setPacketHandled(true);
    }

    public static void sendBaseListToPlayer(ServerPlayer player) {
        ServerLevel currentLevel = player.serverLevel();
        MinecraftServer server = player.server;
        UUID playerId = player.getUUID();
        BlockPos playerPos = player.blockPosition();
        String currentDimensionId = dimensionId(currentLevel);

        RemoteTerminalBaseIndex index = RemoteTerminalBaseIndex.get(server);
        Map<String, RemoteTerminalBaseInfo> merged = new LinkedHashMap<>();

        for (RemoteTerminalBaseIndex.BaseRecord record : index.getByOwner(playerId)) {
            RemoteTerminalBaseInfo info = resolveIndexedEntry(server, index, playerId, record);
            if (info != null) {
                merged.put(buildKey(info.dimensionId(), info.pos()), info);
            }
        }

        collectLoadedCurrentDimensionBases(currentLevel, playerPos, playerId, index, merged);

        List<RemoteTerminalBaseInfo> entries = new ArrayList<>(merged.values());
        entries.sort((a, b) -> compareEntries(a, b, playerPos, currentDimensionId));

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RemoteTerminalBaseListPacket(entries));
    }

    private static RemoteTerminalBaseInfo resolveIndexedEntry(
            MinecraftServer server,
            RemoteTerminalBaseIndex index,
            UUID owner,
            RemoteTerminalBaseIndex.BaseRecord record
    ) {
        ServerLevel targetLevel = resolveLevel(server, record.dimensionId());
        BlockPos pos = BlockPos.of(record.posLong());

        if (targetLevel != null && targetLevel.hasChunkAt(pos)) {
            BlockEntity blockEntity = targetLevel.getBlockEntity(pos);
            if (blockEntity instanceof TurretBaseBlockEntity base && owner.equals(base.getOwner())) {
                RemoteTerminalBaseInfo loadedInfo = fromLoadedBase(targetLevel, base);
                index.upsert(targetLevel, pos, owner, base.getTier(), loadedInfo.baseName());
                return loadedInfo;
            }
            index.remove(record.dimensionId(), pos);
            return null;
        }

        return new RemoteTerminalBaseInfo(
                pos,
                record.dimensionId(),
                record.baseName(),
                record.tier(),
                false,
                false,
                (byte) 0b111111,
                List.of(),
                List.of()
        );

    }

    private static void collectLoadedCurrentDimensionBases(
            ServerLevel level,
            BlockPos playerPos,
            UUID owner,
            RemoteTerminalBaseIndex index,
            Map<String, RemoteTerminalBaseInfo> merged
    ) {


        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;

        for (int chunkX = centerChunkX - SEARCH_CHUNK_RADIUS; chunkX <= centerChunkX + SEARCH_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunkZ - SEARCH_CHUNK_RADIUS; chunkZ <= centerChunkZ + SEARCH_CHUNK_RADIUS; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof TurretBaseBlockEntity base)) {
                        continue;
                    }
                    if (!owner.equals(base.getOwner())) {
                        continue;
                    }
                    RemoteTerminalBaseInfo info = fromLoadedBase(level, base);
                    merged.put(buildKey(info.dimensionId(), info.pos()), info);
                    index.upsert(level, base.getBlockPos(), owner, base.getTier(), info.baseName());
                }
            }
        }
    }

    private static RemoteTerminalBaseInfo fromLoadedBase(ServerLevel level, TurretBaseBlockEntity base) {
        ItemStack chipStack = base.getPluginStack();
        boolean hasSmartChip = !chipStack.isEmpty() && chipStack.getItem() instanceof SmartChipItem;
        List<String> blacklist = hasSmartChip ? SmartChipItem.getBlacklist(chipStack) : List.of();
        List<String> whitelist = hasSmartChip ? SmartChipItem.getWhitelist(chipStack) : List.of();
        return new RemoteTerminalBaseInfo(
                base.getBlockPos(),
                dimensionId(level),
                base.getDisplayName().getString(),
                base.getTier(),
                true,
                hasSmartChip,
                base.getEnabledFacesMask(),
                blacklist,
                whitelist
        );

    }

    private static int compareEntries(RemoteTerminalBaseInfo a, RemoteTerminalBaseInfo b, BlockPos playerPos, String currentDimensionId) {
        int sameDimCompare = Boolean.compare(!a.dimensionId().equals(currentDimensionId), !b.dimensionId().equals(currentDimensionId));
        if (sameDimCompare != 0) {
            return sameDimCompare;
        }

        int loadedCompare = Boolean.compare(!a.loaded(), !b.loaded());
        if (loadedCompare != 0) {
            return loadedCompare;
        }

        if (a.dimensionId().equals(currentDimensionId) && b.dimensionId().equals(currentDimensionId)) {
            int distanceCompare = Double.compare(a.pos().distSqr(playerPos), b.pos().distSqr(playerPos));
            if (distanceCompare != 0) {
                return distanceCompare;
            }
        }

        int dimCompare = a.dimensionId().compareTo(b.dimensionId());
        if (dimCompare != 0) {
            return dimCompare;
        }

        return Long.compare(a.pos().asLong(), b.pos().asLong());
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    private static String buildKey(String dimensionId, BlockPos pos) {
        return dimensionId + "|" + pos.asLong();
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimensionId) {
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);
        if (location == null) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
        return server.getLevel(key);
    }
}
