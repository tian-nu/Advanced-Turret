package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

/**
 * 炮塔公共目标过滤工具。
 *
 * <p>统一处理智能芯片筛选、友伤保护、范围检查和厉行节约逻辑，
 * 避免多个炮塔重复维护同一套目标判定规则。</p>
 */
public final class TurretTargetFilterHelper {

    private TurretTargetFilterHelper() {
    }

    /**
     * 执行各炮塔共用的基础目标过滤。
     */
    public static boolean passesCommonChecks(LivingEntity entity,
                                             TurretBaseBlockEntity base,
                                             BlockPos pos,
                                             double searchRadius) {
        if (!isAttackable(entity)) {
            return false;
        }
        if (!matchesSmartChipRules(entity, base)) {
            return false;
        }
        if (isProtectedFriendly(entity, base)) {
            return false;
        }
        return LinearTurretTargetingHelper.isTargetInRange(entity, pos, searchRadius);
    }

    /**
     * 厉行节约模式下，检查该目标是否已经被其他炮塔预约击杀。
     */
    public static boolean shouldSkipForThrifty(LivingEntity entity, TurretBaseBlockEntity base) {
        if (!base.isThriftyMode()) {
            return false;
        }
        float reservedDamage = base.getReservedDamage(entity.getId());
        return entity.getHealth() - reservedDamage <= 0;
    }

    private static boolean isAttackable(LivingEntity entity) {
        return entity.isAlive() && !entity.isInvulnerable();
    }

    private static boolean matchesSmartChipRules(LivingEntity entity, TurretBaseBlockEntity base) {
        ItemStack pluginStack = base.getPluginStack();
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();

        List<String> blacklist = SmartChipItem.getBlacklist(pluginStack);
        boolean inBlacklist = blacklist.contains(entityId);

        List<String> whitelist = SmartChipItem.getWhitelist(pluginStack);
        if (whitelist.contains(entityId)) {
            return false;
        }

        if (inBlacklist) {
            return true;
        }

        int flags = SmartChipItem.getTargetFlags(pluginStack);
        if ((flags & SmartChipItem.FLAG_HOSTILE) != 0 && entity instanceof Enemy) {
            return true;
        }
        if ((flags & SmartChipItem.FLAG_NEUTRAL) != 0 && entity instanceof NeutralMob) {
            return true;
        }
        if ((flags & SmartChipItem.FLAG_FRIENDLY) != 0 &&
            (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal)) {
            return true;
        }
        if ((flags & SmartChipItem.FLAG_PLAYERS) != 0 &&
            entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            return true;
        }
        return false;
    }

    /**
     * 返回 true 表示该实体应当被友伤保护排除。
     */
    private static boolean isProtectedFriendly(LivingEntity entity, TurretBaseBlockEntity base) {
        if (!base.isFriendlyFire()) {
            return false;
        }

        UUID ownerId = base.getOwner();
        if (ownerId == null) {
            return false;
        }
        if (entity.getUUID().equals(ownerId)) {
            return true;
        }
        if (entity instanceof TamableAnimal tameable) {
            UUID tameOwner = tameable.getOwnerUUID();
            return tameOwner != null && tameOwner.equals(ownerId);
        }
        return false;
    }
}