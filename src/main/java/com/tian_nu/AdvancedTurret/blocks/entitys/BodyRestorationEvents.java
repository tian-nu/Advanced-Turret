package com.tian_nu.AdvancedTurret.blocks.entitys;

import com.tian_nu.AdvancedTurret.Config;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 身躯复原炮塔的死亡事件处理。
 *
 * <p>玩家死亡时，由范围内、芯片白名单中、且能量与储液罐充足的身躯复原炮塔将其
 * 原地复原（类似不死图腾）。同一玩家在冷却 tick 内仅可复原一次，防止连续死亡循环。</p>
 */
public class BodyRestorationEvents {

    /** 玩家 UUID → 上次复原时的游戏时间（tick）。全局共享，跨所有复原炮塔。 */
    private static final Map<UUID, Long> LAST_REVIVE_TICKS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        Long last = LAST_REVIVE_TICKS.get(player.getUUID());
        if (last != null && now - last < Config.bodyRestorationReviveCooldownTicks) {
            return; // 冷却期内：本次死亡成立
        }

        // 就近遍历已加载的复原炮塔，第一个满足全部条件的生效
        List<BodyRestorationTurretBlockEntity> candidates = BodyRestorationTurretBlockEntity.ACTIVE_TURRETS.stream()
                .filter(turret -> turret.getLevel() == level && turret.hasLevel())
                .sorted(Comparator.comparingDouble(turret ->
                        player.distanceToSqr(turret.getBlockPos().getX() + 0.5D,
                                turret.getBlockPos().getY() + 0.5D,
                                turret.getBlockPos().getZ() + 0.5D)))
                .toList();

        boolean revived = false;
        for (BodyRestorationTurretBlockEntity turret : candidates) {
            if (turret.tryRevive(player)) {
                revived = true;
                break;
            }
        }
        if (!revived) {
            return;
        }

        // 取消死亡：玩家原地复原，保留物品与经验（Player.die 中该事件最先触发，
        // 取消后掉落/经验/死亡界面全部不再执行）
        event.setCanceled(true);
        LAST_REVIVE_TICKS.put(player.getUUID(), now);

        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.setRemainingFireTicks(0);
        player.fallDistance = 0.0F;
        player.setAirSupply(player.getMaxAirSupply());

        // 复原演出：与原版不死图腾相同的粒子与音效
        player.connection.send(new ClientboundEntityEventPacket(player, (byte) 35));
    }
}
