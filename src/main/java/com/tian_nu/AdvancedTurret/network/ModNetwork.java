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
    }
}
