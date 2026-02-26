package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.gui.TurretFaceConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TurretFaceSelectPacket(BlockPos pos, int faceIndex) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(faceIndex);
    }

    public static TurretFaceSelectPacket decode(FriendlyByteBuf buf) {
        return new TurretFaceSelectPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof TurretFaceConfigMenu menu)) return;
            if (!menu.getBlockEntity().getBlockPos().equals(pos)) return;
        });
        context.setPacketHandled(true);
    }
}
