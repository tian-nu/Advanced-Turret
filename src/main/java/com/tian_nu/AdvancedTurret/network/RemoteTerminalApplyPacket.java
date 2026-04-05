package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 远程终端应用配置请求（黑白名单 + 启用面）。
 */
public record RemoteTerminalApplyPacket(
        String dimensionId,
        BlockPos pos,
        List<String> blacklist,
        List<String> whitelist,
        byte enabledFacesMask
) {

    public static void encode(RemoteTerminalApplyPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.dimensionId);
        buf.writeBlockPos(packet.pos);
        buf.writeCollection(packet.blacklist, FriendlyByteBuf::writeUtf);
        buf.writeCollection(packet.whitelist, FriendlyByteBuf::writeUtf);
        buf.writeByte(packet.enabledFacesMask);
    }

    public static RemoteTerminalApplyPacket decode(FriendlyByteBuf buf) {
        return new RemoteTerminalApplyPacket(
                buf.readUtf(),
                buf.readBlockPos(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                buf.readByte()
        );
    }

    public static void handle(RemoteTerminalApplyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel targetLevel = resolveLevel(player, packet.dimensionId);
            if (targetLevel == null || !targetLevel.isLoaded(packet.pos)) {
                sendResult(player, "gui.advanced_turret.remote_terminal.op_fail_unloaded", -1, -1);
                return;
            }

            BlockEntity blockEntity = targetLevel.getBlockEntity(packet.pos);
            if (!(blockEntity instanceof TurretBaseBlockEntity base)) {
                sendResult(player, "gui.advanced_turret.remote_terminal.op_fail_not_found", -1, -1);
                return;
            }
            if (!canEditBase(player, base)) {
                sendResult(player, "gui.advanced_turret.remote_terminal.op_fail_not_owner", -1, -1);
                return;
            }

            ItemStack chipStack = base.getPluginStack();
            if (chipStack.isEmpty() || !(chipStack.getItem() instanceof SmartChipItem)) {
                sendResult(player, "gui.advanced_turret.remote_terminal.op_fail_no_chip", -1, -1);
                return;
            }

            List<String> sanitizedBlacklist = sanitizeEntries(packet.blacklist, 64);
            List<String> sanitizedWhitelist = sanitizeEntries(packet.whitelist, 64);

            CompoundTag tag = chipStack.getOrCreateTag();
            tag.remove(SmartChipItem.KEY_BLACKLIST);
            for (String value : sanitizedBlacklist) {
                SmartChipItem.addToBlacklist(chipStack, value);
            }

            tag.remove(SmartChipItem.KEY_WHITELIST);
            for (String value : sanitizedWhitelist) {
                addStringToTagList(chipStack, SmartChipItem.KEY_WHITELIST, value);
            }

            base.setEnabledFacesMask(packet.enabledFacesMask);

            sendResult(player, "gui.advanced_turret.remote_terminal.op_apply_success", sanitizedBlacklist.size(), sanitizedWhitelist.size());
            base.setChanged();
            base.syncToClient();
            RemoteTerminalQueryPacket.sendBaseListToPlayer(player);
        });
        context.setPacketHandled(true);
    }

    private static ServerLevel resolveLevel(ServerPlayer player, String dimensionId) {
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);
        if (location == null) {
            return null;
        }
        return player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
    }

    private static List<String> sanitizeEntries(List<String> raw, int limit) {
        Set<String> unique = new LinkedHashSet<>();
        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > 128) {
                trimmed = trimmed.substring(0, 128);
            }
            unique.add(trimmed);
            if (unique.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private static void addStringToTagList(ItemStack stack, String key, String value) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list;
        if (tag.contains(key)) {
            list = tag.getList(key, Tag.TAG_STRING);
        } else {
            list = new ListTag();
        }
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.getString(i))) {
                return;
            }
        }
        list.add(StringTag.valueOf(value));
        tag.put(key, list);
    }

    private static void sendResult(ServerPlayer player, String messageKey, int arg1, int arg2) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RemoteTerminalOperationResultPacket(messageKey, arg1, arg2)
        );
    }

    private static boolean canEditBase(ServerPlayer player, TurretBaseBlockEntity base) {
        UUID owner = base.getOwner();
        return owner == null || owner.equals(player.getUUID());
    }
}
