package com.tian_nu.AdvancedTurret.blocks;

import com.tian_nu.AdvancedTurret.Config;
import com.tian_nu.AdvancedTurret.blocks.entitys.TurretBaseBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 炮塔与基座 tooltip 公共方法。
 */
public final class TurretTooltipHelper {

    private TurretTooltipHelper() {
    }

    public static void addPlacementTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.advanced_turret.turret.place_on_base").withStyle(ChatFormatting.GRAY));
    }

    public static void addGrayLine(List<Component> tooltip, String key, Object... args) {
        tooltip.add(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
    }

    public static void addDarkGrayLine(List<Component> tooltip, String key, Object... args) {
        tooltip.add(Component.translatable(key, args).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void addBaseTooltip(ItemStack stack, List<Component> tooltip) {
        int tier = getBaseTier(stack);
        if (tier <= 0) {
            return;
        }

        addGrayLine(tooltip, "tooltip.advanced_turret.turret_base.common");
        addGrayLine(tooltip, "tooltip.advanced_turret.turret_base.capacity",
                getBaseCapacity(tier),
                getBaseTransferRate(tier));
        addGrayLine(tooltip, "tooltip.advanced_turret.turret_base.slots",
                getBasePluginSlots(tier),
                getBaseUpgradeSlots(tier));

        if (tier >= 5) {
            addDarkGrayLine(tooltip, "tooltip.advanced_turret.turret_base.builtin_smart_chip");
        }
    }

    private static int getBaseTier(ItemStack stack) {
        if (stack.is(ModBlocks.TURRET_BASE_T1.get().asItem())) return 1;
        if (stack.is(ModBlocks.TURRET_BASE_T2.get().asItem())) return 2;
        if (stack.is(ModBlocks.TURRET_BASE_T3.get().asItem())) return 3;
        if (stack.is(ModBlocks.TURRET_BASE_T4.get().asItem())) return 4;
        if (stack.is(ModBlocks.TURRET_BASE_T5.get().asItem())) return 5;
        return 0;
    }

    private static int getBaseCapacity(int tier) {
        int index = Math.max(0, Math.min(TurretBaseBlockEntity.MAX_ENERGIES.length - 1, tier - 1));
        return TurretBaseBlockEntity.MAX_ENERGIES[index];
    }

    private static int getBaseTransferRate(int tier) {
        return switch (tier) {
            case 1 -> Config.turretBaseMaxTransferRateT1;
            case 2 -> Config.turretBaseMaxTransferRateT2;
            case 3 -> Config.turretBaseMaxTransferRateT3;
            case 4 -> Config.turretBaseMaxTransferRateT4;
            case 5 -> Config.turretBaseMaxTransferRateT5;
            default -> TurretBaseBlockEntity.MAX_TRANSFER_RATE;
        };
    }

    private static int getBasePluginSlots(int tier) {
        return switch (tier) {
            case 1 -> 0;
            case 2, 3 -> 1;
            case 4, 5 -> 2;
            default -> 0;
        };
    }

    private static int getBaseUpgradeSlots(int tier) {
        return switch (tier) {
            case 1 -> 0;
            case 2 -> 1;
            case 3, 4 -> 2;
            case 5 -> 3;
            default -> 0;
        };
    }
}