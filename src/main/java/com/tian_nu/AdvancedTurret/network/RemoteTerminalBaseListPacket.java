package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.client.RemoteTerminalClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端返回远程终端基座列表。
 */
public record RemoteTerminalBaseListPacket(List<RemoteTerminalBaseInfo> entries) {

    public static void encode(RemoteTerminalBaseListPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entries.size());
        for (RemoteTerminalBaseInfo entry : packet.entries) {
            buf.writeBlockPos(entry.pos());
            buf.writeUtf(entry.dimensionId());
            buf.writeUtf(entry.baseName());
            buf.writeVarInt(entry.tier());
            buf.writeBoolean(entry.loaded());
            buf.writeBoolean(entry.hasSmartChip());
            buf.writeByte(entry.enabledFacesMask());
            buf.writeCollection(entry.blacklist(), FriendlyByteBuf::writeUtf);
            buf.writeCollection(entry.whitelist(), FriendlyByteBuf::writeUtf);

        }
    }

    public static RemoteTerminalBaseListPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<RemoteTerminalBaseInfo> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new RemoteTerminalBaseInfo(
                    buf.readBlockPos(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readByte(),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf)
            ));

        }
        return new RemoteTerminalBaseListPacket(entries);
    }

    public static void handle(RemoteTerminalBaseListPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                RemoteTerminalClientHooks.handleBaseList(packet.entries())));
        context.setPacketHandled(true);
    }
}
