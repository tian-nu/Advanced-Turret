package com.tian_nu.AdvancedTurret.network;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * 远程终端中的炮塔基座数据。
 */
public record RemoteTerminalBaseInfo(
        BlockPos pos,
        String dimensionId,
        String baseName,
        int tier,
        boolean loaded,
        boolean hasSmartChip,
        byte enabledFacesMask,
        List<String> blacklist,
        List<String> whitelist
) {
}

