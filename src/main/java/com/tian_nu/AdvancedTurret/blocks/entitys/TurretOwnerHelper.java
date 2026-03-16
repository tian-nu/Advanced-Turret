package com.tian_nu.AdvancedTurret.blocks.entitys;

import net.minecraft.nbt.CompoundTag;

/**
 * 炮塔主人信息辅助工具
 */
public final class TurretOwnerHelper {

    private TurretOwnerHelper() {
    }

    public static void saveOwnerNameFromBase(CompoundTag tag, TurretBaseBlockEntity base) {
        if (tag == null || base == null) {
            return;
        }

        String ownerName = base.getResolvedOwnerName();
        if (!ownerName.isEmpty()) {
            tag.putString("OwnerName", ownerName);
        }
    }
}
