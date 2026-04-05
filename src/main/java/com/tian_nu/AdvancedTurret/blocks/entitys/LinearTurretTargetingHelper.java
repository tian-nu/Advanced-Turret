package com.tian_nu.AdvancedTurret.blocks.entitys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 直线炮塔共用的索敌底层工具。
 */
public final class LinearTurretTargetingHelper {

    private LinearTurretTargetingHelper() {
    }

    /**
     * 检查目标是否在搜索范围内。
     */
    public static boolean isTargetInRange(LivingEntity entity, BlockPos turretPos, double searchRadius) {
        Vec3 center = new Vec3(turretPos.getX() + 0.5, turretPos.getY() + 0.5, turretPos.getZ() + 0.5);
        return entity.distanceToSqr(center) <= searchRadius * searchRadius;
    }

    /**
     * 获取可见瞄准点，优先顺序：头部 > 身体 > 脚部。
     */
    @Nullable
    public static Vec3 findVisibleTargetPoint(Level level, BlockPos turretPos, Direction facing, Vec3 muzzlePos, LivingEntity entity) {
        Vec3 headPoint = entity.position().add(0, entity.getEyeHeight(), 0);
        if (canSeePoint(level, turretPos, facing, muzzlePos, headPoint)) {
            return headPoint;
        }

        Vec3 bodyPoint = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        if (canSeePoint(level, turretPos, facing, muzzlePos, bodyPoint)) {
            return bodyPoint;
        }

        Vec3 feetPoint = entity.position();
        if (canSeePoint(level, turretPos, facing, muzzlePos, feetPoint)) {
            return feetPoint;
        }

        return null;
    }

    /**
     * 检查炮口到目标点之间是否有方块阻挡。
     */
    public static boolean canSeePoint(Level level, BlockPos turretPos, Direction facing, Vec3 start, Vec3 end) {
        Vec3 toPoint = end.subtract(start);
        if (toPoint.lengthSqr() < 1.0E-6) {
            return true;
        }

        // 沿炮塔安装面把起点推出去，避免斜向射线仍从自身碰撞箱内开始。
        Vec3 adjustedStart = start.add(
                facing.getStepX() * 0.65D,
                facing.getStepY() * 0.65D,
                facing.getStepZ() * 0.65D
        );
        var hitResult = level.clip(new ClipContext(
                adjustedStart,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        ));

        if (hitResult.getType() == HitResult.Type.MISS) {
            return true;
        }

        return false;
    }
}
