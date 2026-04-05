package com.tian_nu.AdvancedTurret.network;

import com.tian_nu.AdvancedTurret.TurretMod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络系统管理类
 *
 * <p>负责注册和管理所有网络数据包</p>
 *
 * @author tian_nu
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            TurretMod.location("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 注册所有网络数据包
     */
    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                SmartChipConfigPacket.class,
                SmartChipConfigPacket::encode,
                SmartChipConfigPacket::decode,
                SmartChipConfigPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TurretOpenFaceConfigPacket.class,
                TurretOpenFaceConfigPacket::encode,
                TurretOpenFaceConfigPacket::decode,
                TurretOpenFaceConfigPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TurretFaceSelectPacket.class,
                TurretFaceSelectPacket::encode,
                TurretFaceSelectPacket::decode,
                TurretFaceSelectPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TurretRangeConfigPacket.class,
                TurretRangeConfigPacket::encode,
                TurretRangeConfigPacket::decode,
                TurretRangeConfigPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TurretFaceEnableConfigPacket.class,
                TurretFaceEnableConfigPacket::encode,
                TurretFaceEnableConfigPacket::decode,
                TurretFaceEnableConfigPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                RemoteTerminalQueryPacket.class,
                RemoteTerminalQueryPacket::encode,
                RemoteTerminalQueryPacket::decode,
                RemoteTerminalQueryPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                RemoteTerminalApplyPacket.class,
                RemoteTerminalApplyPacket::encode,
                RemoteTerminalApplyPacket::decode,
                RemoteTerminalApplyPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                RemoteTerminalBaseListPacket.class,
                RemoteTerminalBaseListPacket::encode,
                RemoteTerminalBaseListPacket::decode,
                RemoteTerminalBaseListPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                RemoteTerminalOperationResultPacket.class,
                RemoteTerminalOperationResultPacket::encode,
                RemoteTerminalOperationResultPacket::decode,
                RemoteTerminalOperationResultPacket::handle
        );
    }
}
