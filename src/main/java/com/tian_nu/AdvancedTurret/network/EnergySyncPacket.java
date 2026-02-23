package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 能量同步数据包
 * 
 * <p>用于将服务端的能量数据同步到客户端</p>
 * 
 * @author tian_nu
 */
public record EnergySyncPacket(BlockPos pos, int energy, int maxEnergy) {
    
    /**
     * 编码数据包
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(energy);
        buf.writeInt(maxEnergy);
    }
    
    /**
     * 解码数据包
     */
    public static EnergySyncPacket decode(FriendlyByteBuf buf) {
        return new EnergySyncPacket(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt()
        );
    }
    
    /**
     * 处理数据包
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(this));
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * 客户端处理逻辑
     */
    private static void handleClient(EnergySyncPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        
        BlockEntity blockEntity = level.getBlockEntity(packet.pos);
        if (blockEntity instanceof TurretBaseBlockEntity turret) {
            // 更新客户端能量显示
            // 实际能量存储在服务端，这里仅用于GUI显示同步
        }
    }
}
