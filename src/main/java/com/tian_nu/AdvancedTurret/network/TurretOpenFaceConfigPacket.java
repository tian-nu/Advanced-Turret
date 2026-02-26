package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import com.tian_nu.AdvancedTurret.gui.TurretFaceConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record TurretOpenFaceConfigPacket(BlockPos pos) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static TurretOpenFaceConfigPacket decode(FriendlyByteBuf buf) {
        return new TurretOpenFaceConfigPacket(buf.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            BlockEntity be = player.level().getBlockEntity(pos);
            if (!(be instanceof TurretBaseBlockEntity base)) return;

            NetworkHooks.openScreen(
                    player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new TurretFaceConfigMenu(id, inv, base),
                            Component.translatable("container.turret_face_config")
                    ),
                    buf -> buf.writeBlockPos(pos)
            );
        });
        context.setPacketHandled(true);
    }
}

