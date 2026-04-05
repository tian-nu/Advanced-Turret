package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.client.RemoteTerminalClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 远程终端操作结果回包。
 */
public record RemoteTerminalOperationResultPacket(
        String messageKey,
        int arg1,
        int arg2
) {

    public static void encode(RemoteTerminalOperationResultPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.messageKey());
        buf.writeInt(packet.arg1());
        buf.writeInt(packet.arg2());
    }

    public static RemoteTerminalOperationResultPacket decode(FriendlyByteBuf buf) {
        return new RemoteTerminalOperationResultPacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(RemoteTerminalOperationResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                RemoteTerminalClientHooks.handleOperationResult(packet.messageKey(), packet.arg1(), packet.arg2())));
        context.setPacketHandled(true);
    }
}
