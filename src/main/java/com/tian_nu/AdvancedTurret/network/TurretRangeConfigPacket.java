package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 炮塔手动攻击范围配置包。
 */
public record TurretRangeConfigPacket(BlockPos pos, double manualRangeLimit) {

    public static void encode(TurretRangeConfigPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeDouble(packet.manualRangeLimit);
    }

    public static TurretRangeConfigPacket decode(FriendlyByteBuf buf) {
        return new TurretRangeConfigPacket(buf.readBlockPos(), buf.readDouble());
    }

    public static void handle(TurretRangeConfigPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);
            if (!(blockEntity instanceof TurretBaseBlockEntity base)) {
                return;
            }
            if (player.distanceToSqr(packet.pos.getCenter()) > 64.0D) {
                return;
            }
            if (!canEditBase(player, base)) {
                return;
            }
            base.setManualRangeLimit(packet.manualRangeLimit);
        });
        context.setPacketHandled(true);
    }

    private static boolean canEditBase(ServerPlayer player, TurretBaseBlockEntity base) {
        UUID owner = base.getOwner();
        return owner == null || owner.equals(player.getUUID());
    }
}
